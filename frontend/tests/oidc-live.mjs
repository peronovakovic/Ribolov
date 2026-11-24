import assert from 'node:assert/strict'
import { chromium } from 'playwright'
import { createServer, request } from 'node:http'
import { readFile, mkdir, open } from 'node:fs/promises'
import { resolve, dirname, extname, sep } from 'node:path'
import { fileURLToPath } from 'node:url'
import { randomBytes, createHmac } from 'node:crypto'
import { spawn } from 'node:child_process'

// Creates only a uniquely named test realm. Requires an isolated development/CI database.
const root=resolve(dirname(fileURLToPath(import.meta.url)),'../..')
const keycloak=process.env.TEST_KEYCLOAK_URL??'http://localhost:8180'
assert.ok(['localhost','127.0.0.1'].includes(new URL(keycloak).hostname),'Use a local test identity provider')
for(const key of ['IT_DATABASE_URL','IT_DATABASE_USERNAME','IT_DATABASE_PASSWORD','IT_MIGRATION_USERNAME','IT_MIGRATION_PASSWORD','TEST_KEYCLOAK_ADMIN_PASSWORD'])assert.ok(process.env[key],key+' is required')
const realmName='ribolov-e2e-'+randomBytes(6).toString('hex'),clientSecret=randomBytes(24).toString('hex'),password=randomBytes(24).toString('hex')
const email='login-'+randomBytes(4).toString('hex')+'@example.test'
let adminToken,ownedRealm=false,backend,browser,web,logFile,otpSecret,backendError
async function waitFor(url) {
 for(let i=0;i<180;i++){if(backendError || (backend && backend.exitCode!==null))throw new Error('Test backend failed to start');try{if((await fetch(url,{signal:AbortSignal.timeout(1000)})).ok)return}catch{}
  await new Promise(r=>setTimeout(r,500))}
 throw new Error('Service did not become ready: '+url)
}
async function kc(path,method='GET',body){
 let r=await fetch(keycloak+'/admin/'+path,{method,headers:{Authorization:'Bearer '+adminToken,'Content-Type':'application/json'},body:body===undefined?undefined:JSON.stringify(body)})
 if(r.status===401){
  const refreshed=await fetch(keycloak+'/realms/master/protocol/openid-connect/token',{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:new URLSearchParams({grant_type:'password',client_id:'admin-cli',username:process.env.TEST_KEYCLOAK_ADMIN_USERNAME??'admin',password:process.env.TEST_KEYCLOAK_ADMIN_PASSWORD})})
  assert.ok(refreshed.ok,'Test administrator refresh failed');adminToken=(await refreshed.json()).access_token
  r=await fetch(keycloak+'/admin/'+path,{method,headers:{Authorization:'Bearer '+adminToken,'Content-Type':'application/json'},body:body===undefined?undefined:JSON.stringify(body)})
 }
 assert.ok(r.ok,'Keycloak administration failed: '+method+' '+path+' ('+r.status+')')
 return r
}
function totp(secret){
 const alphabet='ABCDEFGHIJKLMNOPQRSTUVWXYZ234567'
 const bits=[...secret.toUpperCase().replace(/\s|=/g,'')].map(c=>{assert.ok(alphabet.includes(c));return alphabet.indexOf(c).toString(2).padStart(5,'0')}).join('')
 const bytes=Buffer.from((bits.match(/.{8}/g)??[]).map(b=>parseInt(b,2)))
 const counter=Buffer.alloc(8);counter.writeBigUInt64BE(BigInt(Math.floor(Date.now()/30000)))
 const digest=createHmac('sha1',bytes).update(counter).digest(),offset=digest.at(-1)&15
 return String((digest.readUInt32BE(offset)&0x7fffffff)%1000000).padStart(6,'0')
}
async function authenticate(page,renew=false){
 await page.goto('http://localhost:5173/oauth2/authorization/keycloak')
 let submittedOtp=false
 for(let step=0;step<30;step++){
  if(page.url().startsWith('http://localhost:5173/')&&!page.url().includes('/oauth2/')&&!page.url().includes('/login/')){
   assert.ok(submittedOtp,'MFA must be exercised during '+(renew?'renewal':'login'));return
  }
  const secret=page.locator('#kc-totp-secret-key')
  if(await secret.isVisible()){
   otpSecret=await secret.innerText()
   await page.locator('#totp').fill(totp(otpSecret))
   const label=page.locator('#userLabel');if(await label.isVisible())await label.fill('CI authenticator')
   await page.locator('input[type=submit], button[type=submit]').first().click()
   submittedOtp=true
  }else if(await page.locator('#username').isVisible()){
   await page.locator('#username').fill(email);await page.locator('#password').fill(password)
   await page.locator('#kc-login').click()
  }else if(await page.locator('#otp').isVisible()){
   assert.ok(otpSecret,'OTP setup must have completed')
   await page.locator('#otp').fill(totp(otpSecret));await page.locator('#kc-login').click();submittedOtp=true
  }else if(await page.locator('#mode-manual').isVisible()){
   await page.locator('#mode-manual').click()
  }else if(await page.getByRole('link',{name:/Unable to scan/i}).isVisible()){
   await page.getByRole('link',{name:/Unable to scan/i}).click()
  }else await page.waitForTimeout(300)
 }
 throw new Error('OIDC login did not complete')
}
async function assertSession(page){
 const result=await page.evaluate(async()=>{
  const session=await fetch('/api/v1/session').then(r=>r.json())
  const contexts=await fetch('/api/v1/access-contexts').then(r=>r.json())
  const csrf=await fetch('/api/v1/csrf').then(r=>r.json())
  const without=await fetch('/api/v1/invitations/preview',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({token:'invalid'})})
  const withToken=await fetch('/api/v1/invitations/preview',{method:'POST',headers:{'Content-Type':'application/json',[csrf.headerName]:csrf.token},body:JSON.stringify({token:'invalid'})})
  return {session,contexts,without:without.status,withToken:withToken.status}
 })
 assert.ok(result.session.subject)
 assert.deepEqual(result.contexts,[],'First login does not grant business roles')
 assert.equal(result.without,403,'CSRF remains required')
 assert.equal(result.withToken,400,'Signed ID token must contain fresh MFA and verified email before token validation')
}
try{
 await waitFor(keycloak+'/realms/master/.well-known/openid-configuration')
 const response=await fetch(keycloak+'/realms/master/protocol/openid-connect/token',{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},
  body:new URLSearchParams({grant_type:'password',client_id:'admin-cli',username:process.env.TEST_KEYCLOAK_ADMIN_USERNAME??'admin',password:process.env.TEST_KEYCLOAK_ADMIN_PASSWORD})})
 assert.ok(response.ok,'Test Keycloak administrator login failed');adminToken=(await response.json()).access_token
 const realm=JSON.parse(await readFile(resolve(root,'infra/keycloak/realm.json'),'utf8'))
 realm.realm=realmName
 realm.clients.find(c=>c.clientId==='ribolov-web').secret=clientSecret
 realm.users=[{username:email,email,emailVerified:true,enabled:true,firstName:'Test',lastName:'Login',requiredActions:['CONFIGURE_TOTP'],credentials:[{type:'password',value:password,temporary:false}]}]
 await kc('realms','POST',realm);ownedRealm=true
 const dist=resolve(root,'frontend/dist')
 await readFile(resolve(dist,'index.html'))
 web=createServer(async(req,res)=>{
  if(/^\/(api|oauth2|login|logout)(\/|$)/.test(req.url)){
   const upstream=request({hostname:'127.0.0.1',port:8080,path:req.url,method:req.method,headers:req.headers},reply=>{res.writeHead(reply.statusCode,reply.headers);reply.pipe(res)})
   upstream.on('error',()=>{res.writeHead(502);res.end()});req.pipe(upstream);return
  }
  try{
   const path=resolve(dist,'.'+decodeURIComponent(new URL(req.url,'http://localhost').pathname))
   if(path!==dist&&!path.startsWith(dist+sep)){res.writeHead(404);res.end();return}
   const file=path===dist?resolve(dist,'index.html'):path
   res.setHeader('Content-Type',({'.html':'text/html','.js':'text/javascript','.css':'text/css','.svg':'image/svg+xml'})[extname(file)]??'application/octet-stream')
   res.end(await readFile(file))
  }catch{res.writeHead(404);res.end()}
 })
 await new Promise((ok,fail)=>{web.once('error',fail);web.listen(5173,'127.0.0.1',ok)})
 await mkdir(resolve(root,'.cache'),{recursive:true});logFile=await open(resolve(root,'.cache/oidc-backend.log'),'w')
 backend=spawn(process.env.JAVA_HOME?resolve(process.env.JAVA_HOME,'bin',process.platform==='win32'?'java.exe':'java'):'java',
  ['-jar',resolve(root,'backend/target/ribolov-backend-0.1.0-SNAPSHOT.jar')],{
   cwd:root,stdio:['ignore',logFile.fd,logFile.fd],env:{...process.env,
    DATABASE_URL:process.env.IT_DATABASE_URL,DATABASE_USERNAME:process.env.IT_DATABASE_USERNAME,DATABASE_PASSWORD:process.env.IT_DATABASE_PASSWORD,
    MIGRATION_USERNAME:process.env.IT_MIGRATION_USERNAME,MIGRATION_PASSWORD:process.env.IT_MIGRATION_PASSWORD,
    OIDC_ISSUER:keycloak+'/realms/'+realmName,OIDC_WEB_SECRET:clientSecret,WEB_URL:'http://localhost:5173/',SESSION_COOKIE_SECURE:'false',
    SERVER_PORT:'8080',INVITATION_DELIVERY_ENABLED:'false'}})
 backend.once('error',error=>{backendError=error})
 await waitFor('http://localhost:8080/actuator/health/liveness')
 browser=await chromium.launch({headless:true,executablePath:process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH})
 const page=await browser.newPage()
 await authenticate(page);await assertSession(page)
 await authenticate(page,true);await assertSession(page)
 await page.getByRole('button',{name:'Odjava',exact:true}).click()
 await page.getByRole('link',{name:'Prijavite se',exact:true}).waitFor()
 assert.equal(await page.evaluate(async()=> (await fetch('/api/v1/session')).status),401)
 console.log('PASS: real Keycloak password/OTP, signed email/MFA claims, renewal, CSRF, no automatic grants, federated logout')
}finally{
 await browser?.close();if(web)await new Promise(r=>web.close(r));backend?.kill();await logFile?.close()
 if(ownedRealm)await kc('realms/'+realmName,'DELETE')
}
