import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { conversionOutcomeOf } from '@/lib/conversion-outcome'
import { rateBookAfterRemoval, rateBookOf } from '@/lib/rate-book'
import { THE_WORKSHEET_DID_NOT_ANSWER } from '@/lib/refusal-copy'
import { convert, listRates, type DatedRate } from '@/lib/worksheet-api'
import { holdWorksheetKey } from '@/lib/worksheet-key'

function aDatedRate(overrides: Partial<DatedRate> = {}): DatedRate {
  return { id: 'rate-1', from: 'EUR', to: 'PLN', rate: '4.2515', ratedAt: '2026-03-02T12:00:00Z', ...overrides }
}

function aBrowserStorage() {
  const entries = new Map<string, string>()
  return {
    getItem: (entry: string) => entries.get(entry) ?? null,
    setItem: (entry: string, value: string) => void entries.set(entry, value),
    removeItem: (entry: string) => void entries.delete(entry),
  }
}

const theWorksheet = vi.fn<typeof fetch>()

beforeEach(() => {
  vi.stubGlobal('localStorage', aBrowserStorage())
  vi.stubGlobal('fetch', theWorksheet)
})

afterEach(() => {
  theWorksheet.mockReset()
  vi.unstubAllGlobals()
})

describe('the rate book of a worksheet', () => {
  it('a visitor without a key sees how to begin', async () => {
    const book = rateBookOf(await listRates())
    const outcome = conversionOutcomeOf(
      await convert({ amount: '12', from: 'EUR', to: 'USD', at: '2026-03-02T12:00:00Z' }),
    )

    expect(book).toEqual({ kind: 'how-to-begin' })
    expect(outcome).toEqual({ kind: 'how-to-begin' })
    expect(theWorksheet).not.toHaveBeenCalled()
  })

  it('a holder with no rates sees how to begin', async () => {
    holdWorksheetKey('a-key')
    theWorksheet.mockResolvedValue(Response.json({ rates: [], limit: 500 }))

    expect(rateBookOf(await listRates())).toEqual({ kind: 'how-to-begin' })
  })

  it('a holder sees each kept rate with its exact value', async () => {
    holdWorksheetKey('a-key')
    theWorksheet.mockResolvedValue(Response.json({ rates: [aDatedRate({ rate: '4.250000000001' })], limit: 500 }))

    expect(rateBookOf(await listRates())).toEqual({
      kind: 'rates',
      rates: [aDatedRate({ rate: '4.250000000001' })],
      trouble: null,
    })
  })

  it('a silent worksheet is told calmly', async () => {
    holdWorksheetKey('a-key')
    theWorksheet.mockRejectedValue(new TypeError('Failed to fetch'))

    expect(rateBookOf(await listRates())).toEqual({ kind: 'trouble', message: THE_WORKSHEET_DID_NOT_ANSWER })
  })
})

describe('removing a rate', () => {
  const kept = [aDatedRate({ id: 'first' }), aDatedRate({ id: 'second', from: 'PLN', to: 'USD' })]

  it('a removed rate leaves the list', () => {
    expect(rateBookAfterRemoval(kept, 'first', { kind: 'answered', value: null })).toEqual({
      kind: 'rates',
      rates: [kept[1]],
      trouble: null,
    })
  })

  it('removing the last rate shows how to begin', () => {
    expect(rateBookAfterRemoval([kept[0]], 'first', { kind: 'answered', value: null })).toEqual({
      kind: 'how-to-begin',
    })
  })

  it('a rate the worksheet no longer holds leaves the list too', () => {
    const book = rateBookAfterRemoval(kept, 'first', { kind: 'refused', refusal: { code: 'rate_not_found' } })

    expect(book).toMatchObject({ kind: 'rates', rates: [kept[1]] })
  })

  it('a removal the worksheet did not answer keeps the rate and says so', () => {
    expect(rateBookAfterRemoval(kept, 'first', { kind: 'unreachable' })).toEqual({
      kind: 'rates',
      rates: kept,
      trouble: THE_WORKSHEET_DID_NOT_ANSWER,
    })
  })
})
