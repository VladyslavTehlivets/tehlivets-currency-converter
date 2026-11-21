import { describe, expect, it } from 'vitest'

import {
  asksForAnotherAccount,
  noticeForRefusal,
  REFUSAL_CODES,
  safeNextPath,
} from '@/lib/sign-in-refusal'

const queryWith = (written: string) => new URLSearchParams(written)

describe('a refusal comes back as one of six closed codes', () => {
  it.each(REFUSAL_CODES)('the %s code has its own notice', (code) => {
    const notice = noticeForRefusal(queryWith(`refused=${code}&door=google`))
    expect(notice?.title).toBeTruthy()
    expect(notice?.body).toBeTruthy()
  })

  it('a cancelled sign-in is a status that blames nobody', () => {
    expect(noticeForRefusal(queryWith('refused=cancelled&door=google'))).toMatchObject({ tone: 'quiet', role: 'status' })
  })

  it('an account that cannot get in as it is gets the trouble tone and asks for another account', () => {
    for (const code of ['email-unverified', 'not-invited']) {
      const notice = noticeForRefusal(queryWith(`refused=${code}&door=google`))
      expect(notice?.tone).toBe('trouble')
      expect(asksForAnotherAccount(notice)).toBe(true)
    }
    expect(asksForAnotherAccount(noticeForRefusal(queryWith('refused=cancelled&door=google')))).toBe(false)
  })

  it('a code nobody defined says only that the sign-in did not finish', () => {
    expect(noticeForRefusal(queryWith('refused=ada%40example.com&door=google'))?.title).toBe('That sign-in did not finish')
    expect(noticeForRefusal(queryWith('refused=constructor&door=google'))?.title).toBe('That sign-in did not finish')
  })

  it('free text from the address never reaches the notice', () => {
    const notice = noticeForRefusal(queryWith('refused=%3Cb%3Ehello%3C%2Fb%3E&door=google'))
    expect(JSON.stringify(notice)).not.toContain('hello')
  })

  it('an address without a refusal shows no notice', () => {
    expect(noticeForRefusal(queryWith(''))).toBeNull()
    expect(noticeForRefusal(queryWith('next=/rates'))).toBeNull()
  })
})

describe('the next path stays on this site', () => {
  it.each(['/', '/rates', '/result?amount=1&from=EUR'])('keeps %s', (path) => {
    expect(safeNextPath(path)).toBe(path)
  })

  it.each([null, '', 'rates', '//evil.example', '/\\evil.example', 'https://evil.example', '/a\nb'])(
    'turns %j into the home path',
    (path) => {
      expect(safeNextPath(path)).toBe('/')
    },
  )
})
