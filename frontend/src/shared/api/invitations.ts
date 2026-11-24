import { ApiError } from './admin'
export async function invitationRequest<T>(action:'preview'|'accept',token:string):Promise<T>{
  const csrfResponse=await fetch('/api/v1/csrf',{credentials:'same-origin',cache:'no-store'})
  if(!csrfResponse.ok)throw new ApiError('Obnovite prijavu.',csrfResponse.status,true)
  const csrf=await csrfResponse.json()
  if(typeof csrf.headerName!=='string'||typeof csrf.token!=='string')throw new Error('Zaštita zahtjeva nije dostupna.')
  const response=await fetch('/api/v1/invitations/'+action,{method:'POST',credentials:'same-origin',cache:'no-store',
    headers:{'Content-Type':'application/json',[csrf.headerName]:csrf.token},body:JSON.stringify({token})})
  const data=await response.json().catch(()=>({}))
  if(!response.ok)throw new ApiError(data.detail??'Poziv nije dostupan. Provjerite prijavljeni nalog i važenje poziva.',response.status,data.reauthenticate===true||response.status===401)
  return data as T
}
// Keep the link only in this tab while OIDC redirects away; remove it from the address immediately.
const storageKey='ribolov.pending-invitation'
export function captureInvitation():string {
  const fragment=new URLSearchParams(location.hash.slice(1));const token=fragment.get('invite')
  if(token!==null){history.replaceState(null,'',location.pathname+location.search);if(/^[0-9a-f-]{36}\.[A-Za-z0-9_-]{43}$/.test(token)){sessionStorage.setItem(storageKey,token);return token}sessionStorage.removeItem(storageKey);return ''}
  return sessionStorage.getItem(storageKey)??''
}
export function clearInvitation(){sessionStorage.removeItem(storageKey)}
