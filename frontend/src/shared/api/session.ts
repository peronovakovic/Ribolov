export interface SessionView {
  subject: string
  displayName: string | null
}

export async function loadSession(): Promise<SessionView | null> {
  const response = await fetch('/api/v1/session', { credentials: 'same-origin', cache: 'no-store' })
  if (response.status === 401) return null
  if (!response.ok) throw new Error('Pristup portalu trenutno nije dostupan.')
  const data: unknown = await response.json()
  if (typeof data !== 'object' || data === null || !('subject' in data) || typeof data.subject !== 'string') {
    throw new Error('Primljen je neočekivan odgovor. Pokušajte ponovo.')
  }
  return { subject: data.subject, displayName: 'displayName' in data && typeof data.displayName === 'string' ? data.displayName : null }
}

export async function logout(): Promise<void> {
  const response = await fetch('/api/v1/csrf', { credentials: 'same-origin', cache: 'no-store' })
  if (!response.ok) throw new Error('Odjava nije uspjela. Pokušajte ponovo.')
  const data: unknown = await response.json()
  if (typeof data !== 'object' || data === null || !('parameterName' in data) || !('token' in data)
      || typeof data.parameterName !== 'string' || typeof data.token !== 'string') {
    throw new Error('Odjava nije uspjela. Pokušajte ponovo.')
  }
  // A top-level form permits the subsequent identity-provider logout redirect.
  const form = document.createElement('form')
  form.method = 'POST'
  form.action = '/logout'
  const input = document.createElement('input')
  input.type = 'hidden'
  input.name = data.parameterName
  input.value = data.token
  form.append(input)
  document.body.append(form)
  form.submit()
}