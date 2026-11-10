import type { Refusal } from '@/lib/worksheet-api'

export type FormFieldName = 'amount' | 'from' | 'to' | 'at' | 'rate' | 'ratedAt'

export const CONVERSION_FIELDS = ['amount', 'from', 'to', 'at'] as const satisfies readonly FormFieldName[]

export const RATE_FIELDS = ['from', 'to', 'rate', 'ratedAt'] as const satisfies readonly FormFieldName[]

export interface RefusalCopy {
  field: FormFieldName | null
  message: string
}

interface CopyForCode {
  message: string
  besideAnyOf: readonly FormFieldName[]
}

export const THE_WORKSHEET_DID_NOT_ANSWER = 'The worksheet did not answer. Try again.'

const COPY_FOR_CODE: Record<string, CopyForCode> = {
  invalid_amount: {
    message: 'Enter a positive number with a dot for decimals, like 12.50.',
    besideAnyOf: ['amount'],
  },
  invalid_rate: {
    message: 'Enter a positive rate with a dot for decimals, like 4.25.',
    besideAnyOf: ['rate'],
  },
  invalid_currency: { message: 'Choose a currency from the list.', besideAnyOf: ['from'] },
  same_currency: { message: 'Choose two different currencies.', besideAnyOf: ['to'] },
  invalid_moment: {
    message: 'Choose a date and time between 1900 and 2100.',
    besideAnyOf: ['at', 'ratedAt'],
  },
  rate_exists: {
    message: 'You already keep a rate for this direction at this moment. Choose another moment.',
    besideAnyOf: ['ratedAt'],
  },
  worksheet_full: {
    message: 'Your worksheet holds 500 rates. Remove a rate before you keep another.',
    besideAnyOf: [],
  },
  rate_not_found: { message: 'This rate is no longer in your worksheet.', besideAnyOf: [] },
  rate_missing: { message: 'The required direction has no rate.', besideAnyOf: [] },
}

const ANY_OTHER_REFUSAL: CopyForCode = { message: THE_WORKSHEET_DID_NOT_ANSWER, besideAnyOf: [] }

function isOneOf(fields: readonly FormFieldName[], named: string | undefined): named is FormFieldName {
  return fields.some((field) => field === named)
}

export function copyForRefusal(
  refusal: Pick<Refusal, 'code' | 'field'>,
  fieldsOnTheForm: readonly FormFieldName[],
): RefusalCopy {
  const copy = COPY_FOR_CODE[refusal.code] ?? ANY_OTHER_REFUSAL
  const field = isOneOf(fieldsOnTheForm, refusal.field)
    ? refusal.field
    : (copy.besideAnyOf.find((besideField) => fieldsOnTheForm.includes(besideField)) ?? null)
  return { field, message: copy.message }
}

export interface PlacedRefusals {
  besideField: Partial<Record<FormFieldName, string>>
  onTheForm: string | null
}

export function placeRefusals(
  refusals: readonly Pick<Refusal, 'code' | 'field'>[],
  fieldsOnTheForm: readonly FormFieldName[],
): PlacedRefusals {
  return refusals.reduce<PlacedRefusals>(
    (placed, refusal) => {
      const copy = copyForRefusal(refusal, fieldsOnTheForm)
      if (copy.field === null) return { ...placed, onTheForm: placed.onTheForm ?? copy.message }
      return { ...placed, besideField: { [copy.field]: copy.message, ...placed.besideField } }
    },
    { besideField: {}, onTheForm: null },
  )
}
