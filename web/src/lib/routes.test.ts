import { describe, expect, it } from 'vitest'

import { conversionRequestIn, converterHref, rateAddHref, refusalsIn, resultHref } from '@/lib/routes'

const aRequest = { amount: '12.50', from: 'EUR', to: 'USD', at: '2026-03-02T13:00:00+01:00' }

const queryOf = (href: string) => new URL(href, 'http://worksheet').searchParams

describe('a route carries the conversion in its address', () => {
  it('the result reads back the conversion it was sent', () => {
    expect(conversionRequestIn(queryOf(resultHref(aRequest)))).toEqual(aRequest)
  })

  it('the converter reads back the refusal it was sent', () => {
    expect(refusalsIn(queryOf(converterHref(aRequest, { code: 'same_currency', field: 'to' })))).toEqual([
      { code: 'same_currency', field: 'to' },
    ])
  })

  it('a converter address without a refusal refuses nothing', () => {
    expect(refusalsIn(queryOf(converterHref(aRequest)))).toEqual([])
  })

  it('the missing rate address names the direction and moment', () => {
    expect(rateAddHref({ from: 'PLN', to: 'USD' }, aRequest.at)).toBe(
      '/rates/new?from=PLN&to=USD&at=2026-03-02T13%3A00%3A00%2B01%3A00',
    )
  })
})
