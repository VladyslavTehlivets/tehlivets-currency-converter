import { describe, expect, it } from 'vitest'

import { CONVERSION_FIELDS, placeRefusals, RATE_FIELDS, THE_WORKSHEET_DID_NOT_ANSWER } from '@/lib/refusal-copy'
import { converterHref, refusalsIn } from '@/lib/routes'
import { refusalIn } from '@/lib/worksheet-api'

const SERVER_TEXT = 'java.lang.NumberFormatException: <b>1,5</b>'

const aRequest = { amount: '1,5', from: 'USD', to: 'EUR', at: '2026-03-02T12:00:00Z' }

describe('a refusal speaks in the words of the client', () => {
  it('a refusal code becomes field copy and never server text', () => {
    const refusal = refusalIn({ error: { code: 'invalid_amount', field: 'amount', message: SERVER_TEXT } })

    const placed = placeRefusals(refusal ? [refusal] : [], CONVERSION_FIELDS)

    expect(placed.besideField.amount).toBe('Enter a positive number with a dot for decimals, like 12.50.')
    expect(JSON.stringify(placed)).not.toContain('NumberFormatException')
  })

  it('a code the client does not know is told as a calm form notice', () => {
    const placed = placeRefusals([{ code: SERVER_TEXT, field: 'amount; drop' }], CONVERSION_FIELDS)

    expect(placed).toEqual({ besideField: {}, onTheForm: THE_WORKSHEET_DID_NOT_ANSWER })
  })

  it('a refusal named in the address of the converter lands beside its field', () => {
    const address = new URL(converterHref(aRequest, { code: 'invalid_amount', field: 'amount' }), 'http://worksheet')

    const placed = placeRefusals(refusalsIn(address.searchParams), CONVERSION_FIELDS)

    expect(Object.keys(placed.besideField)).toEqual(['amount'])
  })

  it('a moment refusal sits beside the rate date on the rate form', () => {
    const placed = placeRefusals([{ code: 'invalid_moment', field: 'at' }], RATE_FIELDS)

    expect(placed.besideField.ratedAt).toBe('Choose a date and time between 1900 and 2100.')
  })
})
