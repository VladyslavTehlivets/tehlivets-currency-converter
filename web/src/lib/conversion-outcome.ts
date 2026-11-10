import { CONVERSION_FIELDS, copyForRefusal, THE_WORKSHEET_DID_NOT_ANSWER } from '@/lib/refusal-copy'
import type { ConversionReceipt, MissingDirection, Refusal, WorksheetAnswer } from '@/lib/worksheet-api'

export type ConversionOutcome =
  | { kind: 'receipt'; receipt: ConversionReceipt }
  | { kind: 'how-to-begin' }
  | { kind: 'missing-rate'; missing: MissingDirection; at: string | undefined }
  | { kind: 'field-refusal'; refusal: Refusal }
  | { kind: 'trouble'; message: string }

function outcomeOfRefusal(refusal: Refusal): ConversionOutcome {
  if (refusal.code === 'rate_missing' && refusal.missing) {
    return { kind: 'missing-rate', missing: refusal.missing, at: refusal.at }
  }
  const copy = copyForRefusal(refusal, CONVERSION_FIELDS)
  return copy.field ? { kind: 'field-refusal', refusal } : { kind: 'trouble', message: copy.message }
}

export function conversionOutcomeOf(answer: WorksheetAnswer<ConversionReceipt>): ConversionOutcome {
  switch (answer.kind) {
    case 'answered':
      return { kind: 'receipt', receipt: answer.value }
    case 'no-worksheet':
      return { kind: 'how-to-begin' }
    case 'unreachable':
      return { kind: 'trouble', message: THE_WORKSHEET_DID_NOT_ANSWER }
    case 'refused':
      return outcomeOfRefusal(answer.refusal)
  }
}
