import { describe, expect, it } from 'vitest'

import { conversionOutcomeOf } from '@/lib/conversion-outcome'
import { THE_WORKSHEET_DID_NOT_ANSWER } from '@/lib/refusal-copy'
import type { ConversionReceipt } from '@/lib/worksheet-api'

const aCrossReceipt: ConversionReceipt = {
  amount: '12',
  result: '3.003',
  base: 'PLN',
  at: '2026-03-02T12:00:00Z',
  legs: [
    { from: 'EUR', to: 'PLN', rate: '4.25', ratedAt: '2026-03-02T12:00:00Z', amountIn: '12', amountOut: '51.00' },
    { from: 'PLN', to: 'USD', rate: '0.2731', ratedAt: '2026-03-02T12:00:00Z', amountIn: '51.00', amountOut: '13.9281' },
  ],
}

describe('what a conversion answer shows', () => {
  it('an answered conversion shows its receipt as the worksheet wrote it', () => {
    expect(conversionOutcomeOf({ kind: 'answered', value: aCrossReceipt })).toEqual({
      kind: 'receipt',
      receipt: aCrossReceipt,
    })
  })

  it('a missing direction offers exactly that direction and moment', () => {
    const outcome = conversionOutcomeOf({
      kind: 'refused',
      refusal: { code: 'rate_missing', missing: { from: 'PLN', to: 'USD' }, at: '2026-03-02T12:00:00Z' },
    })

    expect(outcome).toEqual({
      kind: 'missing-rate',
      missing: { from: 'PLN', to: 'USD' },
      at: '2026-03-02T12:00:00Z',
    })
  })

  it('a field refusal goes back beside the field on the converter', () => {
    const refusal = { code: 'invalid_amount', field: 'amount' }

    expect(conversionOutcomeOf({ kind: 'refused', refusal })).toEqual({ kind: 'field-refusal', refusal })
  })

  it('a missing rate without its direction is told calmly', () => {
    expect(conversionOutcomeOf({ kind: 'refused', refusal: { code: 'rate_missing' } })).toEqual({
      kind: 'trouble',
      message: 'The required direction has no rate.',
    })
  })

  it('a worksheet that did not answer is told calmly', () => {
    expect(conversionOutcomeOf({ kind: 'unreachable' })).toEqual({
      kind: 'trouble',
      message: THE_WORKSHEET_DID_NOT_ANSWER,
    })
  })
})
