import type { Role } from './access'
export type OrganizationType = 'MINISTRY' | 'FEDERATION' | 'ASSOCIATION'
export interface Organization { id: string; version: number; name: string; type: OrganizationType; status: string; contactEmail: string | null; contactPhone: string | null; parent: { id: string; name: string } | null }
export interface User { id: string; version: number; subject: string; displayName: string; active: boolean }
export interface Assignment { id: string; version: number; user: { id: string; displayName: string }; organization: { id: string; name: string } | null; role: Role; validFrom: string; validUntil: string | null; revokedAt: string | null }
export interface AuditEvent { id: string; occurredAt: string; action: string; actor: string; organization: string | null; targetUser: string | null; role: Role | null; reason: string }
export interface Affiliation { id: string; parent: { id: string; name: string }; validFrom: string; validUntil: string | null }
export interface Page<T> { items: T[]; page: number; hasNext: boolean }
export const organizationTypes: Record<OrganizationType, string> = { MINISTRY: 'Ministarstvo', FEDERATION: 'Savez', ASSOCIATION: 'Udruženje' }
export class ApiError extends Error {
  constructor(message: string, public status: number, public reauthenticate = false) { super(message) }
}
export async function adminRequest<T>(context: string, path: string, method = 'GET', body?: unknown, signal?: AbortSignal): Promise<T> {
  const headers: Record<string, string> = { 'X-Access-Context': context, Accept: 'application/json' }
  if (method !== 'GET') {
    const csrfResponse = await fetch('/api/v1/csrf', { credentials: 'same-origin', cache: 'no-store', signal })
    if (!csrfResponse.ok) throw new ApiError('Sesija je istekla. Prijavite se ponovo.', csrfResponse.status, true)
    const csrf = await csrfResponse.json()
    if (typeof csrf.headerName !== 'string' || typeof csrf.token !== 'string') throw new Error('Zaštita zahtjeva nije dostupna. Osvježite stranicu.')
    headers[csrf.headerName] = csrf.token
    headers['Content-Type'] = 'application/json'
  }
  const response = await fetch('/api/v1/admin' + path, { method, headers, credentials: 'same-origin', cache: 'no-store',
    body: body === undefined ? undefined : JSON.stringify(body), signal })
  if (!response.ok) {
    const problem = await response.json().catch(() => ({}))
    throw new ApiError(typeof problem.detail === 'string' ? problem.detail
      : response.status === 401 ? 'Sesija je istekla. Prijavite se ponovo.'
      : response.status === 403 ? 'Pristup je odbijen. Osvježite ovlaštenja ili obnovite prijavu.'
      : 'Zahtjev nije uspio. Provjerite podatke i pokušajte ponovo.', response.status, problem.reauthenticate === true || response.status === 401)
  }
  return response.json() as Promise<T>
}
export function dateLabel(value: string | null): string {
  return value ? new Date(value).toLocaleString('sr-Latn-BA', { dateStyle: 'medium', timeStyle: 'short' }) : 'Bez krajnjeg roka'
}
