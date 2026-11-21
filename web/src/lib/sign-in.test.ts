import { describe, expect, it } from 'vitest'

import { googleCanReturnHere, refusalNoticeFor, sameOriginPath, signInHref } from '@/lib/sign-in'

describe('a sign-in refusal speaks in closed copy', () => {
  it.each(['cancelled', 'state-mismatch', 'token-for-another-client', 'email-unverified', 'not-invited', 'provider-unavailable'])(
    'the code %s has its own notice',
    (code) => {
      expect(refusalNoticeFor(code)?.title).toBeTruthy()
    },
  )

  it('cancelling is neutral and every other refusal is trouble', () => {
    expect(refusalNoticeFor('cancelled')?.tone).toBe('neutral')
    expect(refusalNoticeFor('not-invited')?.tone).toBe('trouble')
  })

  it('a code the client does not know says the sign-in did not finish and never echoes the code', () => {
    const notice = refusalNoticeFor('<img src=x onerror=alert(1)>')

    expect(notice?.title).toBe('That sign-in did not finish')
    expect(JSON.stringify(notice)).not.toContain('onerror')
  })

  it('an inherited object key is not a code', () => {
    expect(refusalNoticeFor('constructor')?.title).toBe('That sign-in did not finish')
  })

  it('no code means no notice', () => {
    expect(refusalNoticeFor(null)).toBeNull()
  })
})

describe('the way back after sign-in stays on this site', () => {
  it.each([
    ['/rates', '/rates'],
    ['//evil.example', '/'],
    ['/\\evil.example', '/'],
    ['https://evil.example', '/'],
    [null, '/'],
  ])('next %s becomes %s', (candidate, expected) => {
    expect(sameOriginPath(candidate)).toBe(expected)
  })

  it('the sign-in link carries the current path', () => {
    expect(signInHref('/rates')).toBe('/sign-in?next=%2Frates')
    expect(signInHref('/')).toBe('/sign-in')
  })
})

describe('Google can send the person back only to a secure or local address', () => {
  it.each([
    [{ protocol: 'https:', hostname: 'currency.vtehsol.com' }, true],
    [{ protocol: 'http:', hostname: '127.0.0.1' }, true],
    [{ protocol: 'http:', hostname: 'localhost' }, true],
    [{ protocol: 'http:', hostname: '10.13.13.4' }, false],
  ])('%j gives %s', (location, expected) => {
    expect(googleCanReturnHere(location)).toBe(expected)
  })
})
