import { momentFromLocalInput } from '@/lib/moments'
import type { Refusal } from '@/lib/worksheet-api'

const EXACT_DECIMAL = /^[0-9]{1,15}(\.[0-9]{1,12})?$/

export function isPositiveDecimal(written: string): boolean {
  return EXACT_DECIMAL.test(written) && /[1-9]/.test(written)
}

export interface ConversionDraft {
  amount: string
  from: string
  to: string
  at: string
}

export interface RateDraft {
  from: string
  to: string
  rate: string
  ratedAt: string
}

function refusedWhen(refused: boolean, refusal: Refusal): Refusal[] {
  return refused ? [refusal] : []
}

export function checkConversionDraft(draft: ConversionDraft): Refusal[] {
  return [
    ...refusedWhen(!isPositiveDecimal(draft.amount), { code: 'invalid_amount', field: 'amount' }),
    ...refusedWhen(draft.from === draft.to, { code: 'same_currency', field: 'to' }),
    ...refusedWhen(momentFromLocalInput(draft.at) === null, { code: 'invalid_moment', field: 'at' }),
  ]
}

export function checkRateDraft(draft: RateDraft): Refusal[] {
  return [
    ...refusedWhen(draft.from === draft.to, { code: 'same_currency', field: 'to' }),
    ...refusedWhen(!isPositiveDecimal(draft.rate), { code: 'invalid_rate', field: 'rate' }),
    ...refusedWhen(momentFromLocalInput(draft.ratedAt) === null, { code: 'invalid_moment', field: 'ratedAt' }),
  ]
}
