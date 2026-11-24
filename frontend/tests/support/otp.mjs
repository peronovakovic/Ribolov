import assert from 'node:assert/strict'
import { createHmac } from 'node:crypto'

const periodMs = 30_000

// Keycloak development policy: SHA-1, six digits, 30-second periods.
export function totpAt(secret, timeMs) {
  const alphabet = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ234567'
  const normalized = secret.toUpperCase().replace(/\s|=/g, '')
  assert.match(normalized, /^[A-Z2-7]+$/)
  const bits = [...normalized].map(c => alphabet.indexOf(c).toString(2).padStart(5, '0')).join('')
  const bytes = Buffer.from((bits.match(/.{8}/g) ?? []).map(b => parseInt(b, 2)))
  const counter = Buffer.alloc(8)
  counter.writeBigUInt64BE(BigInt(Math.floor(timeMs / periodMs)))
  const digest = createHmac('sha1', bytes).update(counter).digest()
  const offset = digest.at(-1) & 15
  return String((digest.readUInt32BE(offset) & 0x7fffffff) % 1_000_000).padStart(6, '0')
}

// Wait for an unused period and leave time to submit before the code expires.
// This is test orchestration; it does not change the identity provider's replay protection.
export function createOtpGenerator({ now = Date.now, sleep = ms => new Promise(r => setTimeout(r, ms)) } = {}) {
  let lastCounter = -1
  let lastCode
  return async secret => {
    for (;;) {
      const time = now()
      const counter = Math.floor(time / periodMs)
      const remaining = (counter + 1) * periodMs - time
      const code = totpAt(secret, time)
      if (counter > lastCounter && remaining >= 2_000 && code !== lastCode) {
        lastCounter = counter
        lastCode = code
        return code
      }
      await sleep(remaining + 100)
    }
  }
}
