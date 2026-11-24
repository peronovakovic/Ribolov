import test from 'node:test'
import assert from 'node:assert/strict'
import { totpAt, createOtpGenerator } from './support/otp.mjs'

// RFC 6238 Appendix B, SHA-1 examples reduced to six digits.
// https://www.rfc-editor.org/rfc/rfc6238#appendix-B
const secret = 'GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ'

test('matches published TOTP vectors including leading zeros and post-2038 time', () => {
  for (const [seconds, expected] of [[59, '287082'], [1111111109, '081804'],
    [1111111111, '050471'], [1234567890, '005924'], [2000000000, '279037'], [20000000000, '353130']]) {
    assert.equal(totpAt(secret, seconds * 1000), expected)
  }
})

test('immediate renewal waits for a new OTP period instead of replaying a code', async () => {
  let time = 31_000
  const waits = []
  const next = createOtpGenerator({ now: () => time, sleep: async ms => { waits.push(ms); time += ms } })
  assert.equal(await next(secret), totpAt(secret, 31_000))
  assert.equal(await next(secret), totpAt(secret, 60_100))
  assert.deepEqual(waits, [29_100])
})

test('waits across a nearly expired period before entering the code', async () => {
  let time = 59_500
  const waits = []
  const next = createOtpGenerator({ now: () => time, sleep: async ms => { waits.push(ms); time += ms } })
  assert.equal(await next(secret), totpAt(secret, 60_100))
  assert.deepEqual(waits, [600])
})

test('renewal in a later period does not add an unnecessary wait', async () => {
  let time = 31_000
  const next = createOtpGenerator({ now: () => time, sleep: async () => assert.fail('Unexpected wait') })
  await next(secret)
  time = 61_000
  assert.equal(await next(secret), totpAt(secret, time))
})
