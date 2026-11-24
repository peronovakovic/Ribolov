import { chromium } from 'playwright'
import assert from 'node:assert/strict'
import { spawn } from 'node:child_process'
import { mkdir } from 'node:fs/promises'

const uuid = n => '00000000-0000-4000-8000-' + String(n).padStart(12, '0')
const sys = uuid(1), association = uuid(2), orgId = uuid(3), userId = uuid(4)
const date = '2026-11-23T10:00:00Z'
const org = { id: orgId, version: 0, name: 'Udruženje Vrbas', type: 'ASSOCIATION', status: 'ACTIVE', parent: null, contactEmail: 'vrbas@example.test', contactPhone: null }
const contexts = [{ id: sys, role: 'SYSTEM_ADMIN', organization: null, validFrom: date, validUntil: null },
  { id: association, role: 'ASSOCIATION_ADMIN', organization: { id: orgId, name: org.name }, validFrom: date, validUntil: null }]
const calls = [], errors = []
let responseMode = 'ok', created = [], invitations = [], assignments = [{ id: uuid(6), version: 0, user: { id: userId, displayName: 'Novi operater' }, organization: { id: orgId, name: org.name }, role: 'OPERATOR', validFrom: date, validUntil: null, revokedAt: null }], browser
const server = spawn(process.execPath, ['node_modules/vite/bin/vite.js', '--configLoader', 'native', '--host', '127.0.0.1', '--port', '4173', '--strictPort'], { stdio: ['ignore', 'pipe', 'pipe'] })
let serverLog = ''
server.stdout.on('data', b => { serverLog += b })
server.stderr.on('data', b => { serverLog += b })
const checkText = async (page, text) => { await page.getByText(text, { exact: true }).first().waitFor() }
try {
  let ready = false
  for (let i = 0; i < 120; i++) {
    if (server.exitCode !== null) throw new Error(serverLog)
    try { ready = (await fetch('http://127.0.0.1:4173')).ok } catch {}
    if (ready) break
    await new Promise(resolve => setTimeout(resolve, 250))
  }
  assert.ok(ready, serverLog)
  browser = await chromium.launch({ headless: true, executablePath: process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH })
  const page = await browser.newPage({ viewport: { width: 1440, height: 1100 }, timezoneId: 'Europe/Sarajevo' })
  page.on('pageerror', e => errors.push(e.message))
  await page.route(url => url.pathname.startsWith('/api/'), async route => {
    try {
    const req = route.request(), url = new URL(req.url()), path = url.pathname
    const respond = (data, status = 200) => route.fulfill({ status, contentType: 'application/json', body: JSON.stringify(data) })
    if (path === '/api/v1/session') return respond({ subject: 'admin', displayName: 'Test administrator' })
    if (path === '/api/v1/csrf') return respond({ headerName: 'X-CSRF-TOKEN', parameterName: '_csrf', token: 'test-csrf' })
    if (path === '/api/v1/access-contexts') return respond(contexts)
    if(path.startsWith('/api/v1/invitations/')) {
      assert.equal(req.headers()['x-csrf-token'], 'test-csrf')
      assert.equal(req.postDataJSON().token, uuid(20)+'.'+'A'.repeat(43))
      calls.push({ resource: path, body: req.postDataJSON() })
      return respond(path.endsWith('/preview') ? { role:'OPERATOR', organization:org.name, expiresAt:date, validUntil:null } : { assignmentId:uuid(21), role:'OPERATOR', organization:org.name })
    }
    assert.ok(path.startsWith('/api/v1/admin/'), path)
    const context = req.headers()['x-access-context']
    assert.ok([sys, association].includes(context), 'Every administrative request selects an active context')
    const resource = path.replace('/api/v1/admin/', '')
    if (req.method() !== 'GET') {
      assert.equal(req.headers()['x-csrf-token'], 'test-csrf')
      const body = req.postDataJSON()
      assert.ok(body.reason)
      assert.ok(!Object.hasOwn(body, 'code'))
      calls.push({ method: req.method(), resource, context, body })
      if (responseMode === 'mfa') return respond({ detail: 'Obnovite prijavu za ovu promjenu.', reauthenticate: true }, 403)
      if (responseMode === 'conflict') return respond({ detail: 'Podaci su promijenjeni. Osvježite pregled.' }, 409)
      if (resource === 'organizations') {
        const item = { ...org, ...body, id: uuid(5), version: 0, parent: null }
        created.push(item)
        return respond(item, 201)
      }
      if (resource === 'invitations') {
        assert.equal(context,association)
        assert.equal(body.organizationId,orgId)
        const item={id:uuid(15),version:0,...body,organization:org.name,createdAt:date,expiresAt:date,status:'PENDING',sentAt:null,attempts:0,deliveryFailure:null}
        invitations.push(item)
        return respond(item,201)
      }
      if(resource.startsWith('invitations/') && resource.endsWith('/revoke')){
        invitations[0]={...invitations[0],version:1,status:'REVOKED'}
        return respond(invitations[0])
      }
      if (resource === 'roles') {
        assert.equal(context, association)
        assert.equal(body.organizationId, orgId)
        const item = { id: uuid(6), version: 0, user: { id: userId, displayName: 'Novi operater' }, organization: { id: orgId, name: org.name },
          role: body.role, validFrom: date, validUntil: null, revokedAt: null }
        assignments.push(item)
        return respond(item, 201)
      }
      if (resource.endsWith('/revoke')) {
        assignments[0] = { ...assignments[0], version: 1, revokedAt: date }
        return respond(assignments[0])
      }
      return respond({ ...org, ...body, version: 1 })
    }
    const items = resource === 'organizations' ? context === sys ? [org, ...created] : [org]
      : resource === 'users' ? [{ id: userId, version: 0, subject: 'operator-subject', displayName: 'Novi operater', active: true }]
      : resource === 'roles' ? assignments : resource === 'invitations' ? invitations : []
    return respond({ items, page: 0, hasNext: false })
    } catch (error) { errors.push(String(error)); await route.fulfill({status: 500, body: '{}'}) }
  })
  await page.goto('http://127.0.0.1:4173')
  await page.getByLabel('Aktivna uloga').selectOption(sys)
  await checkText(page, org.name)
  await page.getByRole('button', { name: 'Nova organizacija', exact: true }).click()
  await page.getByLabel('Naziv', { exact: true }).fill('Testno ministarstvo')
  await page.locator('.editor').getByLabel('Tip').selectOption('MINISTRY')
  await page.getByLabel('Razlog promjene').fill('Početna registracija')
  await page.getByRole('button', { name: 'Sačuvaj', exact: true }).click()
  await checkText(page, 'Promjena je sačuvana.')
  await checkText(page, 'Testno ministarstvo')
  assert.equal(calls.at(-1).body.parentId, null)
  assert.equal(calls.at(-1).body.type, 'MINISTRY')

  await page.getByLabel('Aktivna uloga').selectOption(association)
  await checkText(page, org.name)
  assert.equal(await page.getByRole('button', { name: 'Nova organizacija', exact: true }).count(), 0)
  assert.equal(await page.getByText('Testno ministarstvo', { exact: true }).count(), 0)
  await page.getByRole('button', { name: 'Uloge', exact: true }).click()
  await page.getByRole('button', { name: 'Opozovi', exact: true }).click()
  await page.getByLabel('Razlog promjene').fill('Prestanak zaduženja')
  await page.getByRole('button', { name: 'Potvrdi opoziv', exact: true }).click()
  await checkText(page, 'Promjena je sačuvana.')
  assert.equal(await page.getByRole('button', { name: 'Opozovi', exact: true }).count(), 0)

  await page.getByRole('button', { name:'Pozivi', exact:true }).click()
  await page.getByRole('button', { name:'Pozovi korisnika', exact:true }).click()
  await page.getByLabel('Adresa e-pošte').fill('member@example.test')
  const roles=await page.locator('.editor').getByLabel('Uloga').locator('option').evaluateAll(options=>options.map(o=>o.value))
  assert.deepEqual(roles,['OPERATOR','WARDEN'])
  await page.getByLabel('Razlog promjene').fill('Novo zaduženje')
  await page.getByRole('button',{name:'Pošalji poziv',exact:true}).click()
  await page.getByRole('cell', {name:/member@example.test/}).waitFor()
  await mkdir('../.cache',{recursive:true})
  await page.screenshot({path:'../.cache/invitations-admin.png',fullPage:true})
  await page.getByRole('button',{name:'Opozovi poziv',exact:true}).click()
  await page.getByLabel('Razlog promjene').fill('Povlačenje')
  await page.getByRole('button',{name:'Potvrdi',exact:true}).click()
  await checkText(page,'Opozvan')

  await page.getByRole('button', { name: 'Organizacije', exact: true }).click()
  await page.getByRole('button', { name: 'Uredi', exact: true }).click()
  await page.getByLabel('Razlog promjene').fill('Izmjena kontakta')
  responseMode = 'conflict'
  await page.getByRole('button', { name: 'Sačuvaj', exact: true }).click()
  await checkText(page, 'Podaci su promijenjeni. Osvježite pregled.')
  await page.getByRole('button', { name: 'Osvježi pregled', exact: true }).click()
  assert.equal(await page.getByLabel('Razlog promjene').count(), 0)
  await page.getByRole('button', { name: 'Uredi', exact: true }).click()
  await page.getByLabel('Razlog promjene').fill('Izmjena kontakta')
  responseMode = 'mfa'
  const before = calls.length
  await page.getByRole('button', { name: 'Sačuvaj', exact: true }).click()
  await page.getByRole('link', { name: 'Obnovi prijavu' }).waitFor()
  assert.equal(calls.length, before + 1, 'Failed writes are not automatically retried')
  assert.equal(await page.getByRole('link', { name: 'Obnovi prijavu' }).getAttribute('href'), '/oauth2/authorization/keycloak')
  assert.equal(calls.at(-1).body.type, undefined)
  await page.getByRole('button', { name: 'Odustani', exact: true }).click()
  responseMode = 'ok'
  await page.getByRole('button', { name: 'Organizacije', exact: true }).click()
  await checkText(page, org.name)
  await mkdir('../.cache', { recursive: true })
  await page.screenshot({ path: '../.cache/admin-desktop.png', fullPage: true })
  await page.setViewportSize({ width: 390, height: 844 })
  await page.getByRole('button', { name: 'Uredi', exact: true }).click()
  await page.screenshot({ path: '../.cache/admin-mobile.png', fullPage: true })
  assert.ok(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), 'Mobile page fits viewport')
  await page.goto('http://127.0.0.1:4173/#invite='+uuid(20)+'.'+'A'.repeat(43))
  await page.getByRole('button',{name:'Prihvati poziv',exact:true}).waitFor()
  await page.screenshot({path:'../.cache/invitation-acceptance.png',fullPage:true})
  assert.equal(new URL(page.url()).hash,'','Invitation secret is removed from the address')
  assert.ok(await page.evaluate(()=>sessionStorage.getItem('ribolov.pending-invitation')),'Link survives an OIDC redirect')
  assert.equal(calls.filter(c=>c.resource==='/api/v1/invitations/accept').length,0,'Opening a link does not consume it')
  await page.reload()
  await page.getByRole('button',{name:'Prihvati poziv',exact:true}).click()
  await page.getByLabel('Aktivna uloga').waitFor()
  assert.equal(calls.filter(c=>c.resource==='/api/v1/invitations/accept').length,1)
  assert.equal(await page.evaluate(()=>sessionStorage.getItem('ribolov.pending-invitation')),null)
  assert.deepEqual(errors, [])
  console.log('PASS: context isolation, organization creation, scoped invitations, role revocation, explicit invitation acceptance, CSRF, conflict refresh, MFA expiry, mobile layout')
} finally {
  await browser?.close()
  server.kill()
}
