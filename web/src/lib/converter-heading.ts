import { CONVERSION_FIELDS, placeRefusals } from '@/lib/refusal-copy'
import type { Refusal } from '@/lib/worksheet-api'

export interface PageHeading {
  title: string
  lede: string
}

const THE_AMOUNT_NEEDS_A_LOOK: PageHeading = {
  title: 'This amount needs another look.',
  lede: 'Use a positive number with a dot for decimals.',
}

const THE_CONVERSION_NEEDS_A_LOOK: PageHeading = {
  title: 'This conversion needs another look.',
  lede: 'Check the field marked below.',
}

export function headingForRefusedConversion(refusals: readonly Refusal[]): PageHeading | null {
  if (refusals.length === 0) return null
  const { besideField } = placeRefusals(refusals, CONVERSION_FIELDS)
  return besideField.amount ? THE_AMOUNT_NEEDS_A_LOOK : THE_CONVERSION_NEEDS_A_LOOK
}
