import { describe, expect, it } from 'vitest'

import { headingForRefusedConversion } from '@/lib/converter-heading'

describe('the heading of a refused conversion', () => {
  it('keeps the converter heading when nothing is refused', () => {
    expect(headingForRefusedConversion([])).toBeNull()
  })

  it('asks for another look at the amount when the amount is refused', () => {
    expect(headingForRefusedConversion([{ code: 'invalid_amount', field: 'amount' }])?.title).toBe(
      'This amount needs another look.',
    )
  })

  it('does not blame the amount when only the currencies are refused', () => {
    expect(headingForRefusedConversion([{ code: 'same_currency', field: 'to' }])?.title).toBe(
      'This conversion needs another look.',
    )
  })
})
