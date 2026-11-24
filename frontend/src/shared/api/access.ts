export type Role = 'SYSTEM_ADMIN' | 'MINISTRY_ANALYST' | 'FEDERATION_ANALYST'
  | 'ASSOCIATION_ADMIN' | 'OPERATOR' | 'WARDEN' | 'ANGLER'

export interface AccessContext {
  id: string
  role: Role
  organization: { id: string; name: string } | null
  validFrom: string
  validUntil: string | null
}

export const roleNames: Record<Role, string> = {
  SYSTEM_ADMIN: 'Administrator sistema',
  MINISTRY_ANALYST: 'Analitičar Ministarstva',
  FEDERATION_ANALYST: 'Analitičar Saveza',
  ASSOCIATION_ADMIN: 'Administrator udruženja',
  OPERATOR: 'Operater udruženja',
  WARDEN: 'Ribočuvar',
  ANGLER: 'Ribolovac',
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null
}

function isContext(value: unknown): value is AccessContext {
  if (!isRecord(value) || typeof value.id !== 'string' || typeof value.role !== 'string'
      || !Object.prototype.hasOwnProperty.call(roleNames, value.role)
      || typeof value.validFrom !== 'string'
      || (value.validUntil !== null && typeof value.validUntil !== 'string')) return false
  return value.organization === null || (isRecord(value.organization)
    && typeof value.organization.id === 'string' && typeof value.organization.name === 'string')
}

export async function loadAccessContexts(): Promise<AccessContext[]> {
  const response = await fetch('/api/v1/access-contexts', { credentials: 'same-origin', cache: 'no-store' })
  if (response.status === 401) throw new Error('Sesija je istekla. Prijavite se ponovo.')
  if (!response.ok) throw new Error('Ovlaštenja trenutno nisu dostupna.')
  const data: unknown = await response.json()
  if (!Array.isArray(data) || !data.every(isContext)) throw new Error('Odgovor o ovlaštenjima nije ispravan.')
  return data
}
