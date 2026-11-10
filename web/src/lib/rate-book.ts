import { copyForRefusal, THE_WORKSHEET_DID_NOT_ANSWER } from '@/lib/refusal-copy'
import type { DatedRate, WorksheetAnswer } from '@/lib/worksheet-api'

export type RateBook =
  | { kind: 'how-to-begin' }
  | { kind: 'rates'; rates: DatedRate[]; trouble: string | null }
  | { kind: 'trouble'; message: string }

const HOW_TO_BEGIN: RateBook = { kind: 'how-to-begin' }

function bookOf(rates: DatedRate[], trouble: string | null): RateBook {
  return rates.length === 0 ? HOW_TO_BEGIN : { kind: 'rates', rates, trouble }
}

function troubleIn(answer: WorksheetAnswer<unknown>): string {
  return answer.kind === 'refused' ? copyForRefusal(answer.refusal, []).message : THE_WORKSHEET_DID_NOT_ANSWER
}

export function rateBookOf(answer: WorksheetAnswer<DatedRate[]>): RateBook {
  if (answer.kind === 'no-worksheet') return HOW_TO_BEGIN
  if (answer.kind === 'answered') return bookOf(answer.value, null)
  return { kind: 'trouble', message: troubleIn(answer) }
}

export function rateBookAfterRemoval(
  rates: DatedRate[],
  removedRateId: string,
  answer: WorksheetAnswer<null>,
): RateBook {
  if (answer.kind === 'no-worksheet') return HOW_TO_BEGIN
  const remaining = rates.filter((rate) => rate.id !== removedRateId)
  if (answer.kind === 'answered') return bookOf(remaining, null)
  if (answer.kind === 'refused' && answer.refusal.code === 'rate_not_found') return bookOf(remaining, null)
  return { kind: 'rates', rates, trouble: troubleIn(answer) }
}
