import { forgetWorksheetKey, heldWorksheetKey, holdWorksheetKey } from '@/lib/worksheet-key'

export interface DatedRate {
  id: string
  from: string
  to: string
  rate: string
  ratedAt: string
}

export interface DatedRateDraft {
  from: string
  to: string
  rate: string
  ratedAt: string
}

export interface ConversionRequest {
  amount: string
  from: string
  to: string
  at: string
}

export interface ConversionLeg {
  from: string
  to: string
  rate: string
  ratedAt: string
  amountIn: string
  amountOut: string
}

export interface ConversionReceipt {
  amount: string
  result: string
  base: string
  at: string
  legs: ConversionLeg[]
}

export interface MissingDirection {
  from: string
  to: string
}

export interface Refusal {
  code: string
  field?: string
  missing?: MissingDirection
  at?: string
}

export type WorksheetAnswer<Value> =
  | { kind: 'answered'; value: Value }
  | { kind: 'refused'; refusal: Refusal }
  | { kind: 'no-worksheet' }
  | { kind: 'unreachable' }

const NO_WORKSHEET = { kind: 'no-worksheet' } as const
const UNREACHABLE = { kind: 'unreachable' } as const

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null
}

function missingDirectionIn(value: unknown): MissingDirection | undefined {
  if (!isRecord(value) || typeof value.from !== 'string' || typeof value.to !== 'string') return undefined
  return { from: value.from, to: value.to }
}

export function refusalIn(body: unknown): Refusal | null {
  if (!isRecord(body) || !isRecord(body.error) || typeof body.error.code !== 'string') return null
  const { code, field, missing, at } = body.error
  return {
    code,
    ...(typeof field === 'string' ? { field } : {}),
    ...(missingDirectionIn(missing) ? { missing: missingDirectionIn(missing) } : {}),
    ...(typeof at === 'string' ? { at } : {}),
  }
}

async function send(path: string, method: string, worksheetKey: string | null, body?: unknown) {
  try {
    return await fetch(path, {
      method,
      headers: {
        Accept: 'application/json',
        ...(body === undefined ? {} : { 'Content-Type': 'application/json' }),
        ...(worksheetKey ? { Authorization: `Bearer ${worksheetKey}` } : {}),
      },
      ...(body === undefined ? {} : { body: JSON.stringify(body) }),
    })
  } catch {
    return null
  }
}

async function answerFrom<Value>(
  response: Response | null,
  readValue: (response: Response) => Promise<Value>,
): Promise<WorksheetAnswer<Value>> {
  if (!response) return UNREACHABLE
  if (response.status === 401) {
    forgetWorksheetKey()
    return NO_WORKSHEET
  }
  if (response.ok) return { kind: 'answered', value: await readValue(response) }
  const refusal = refusalIn(await response.json().catch(() => null))
  return refusal ? { kind: 'refused', refusal } : UNREACHABLE
}

async function startWorksheet(): Promise<string | null> {
  const response = await send('/v1/worksheets', 'POST', null)
  if (!response?.ok) return null
  const body: unknown = await response.json().catch(() => null)
  if (!isRecord(body) || typeof body.worksheetKey !== 'string') return null
  holdWorksheetKey(body.worksheetKey)
  return body.worksheetKey
}

export async function listRates(): Promise<WorksheetAnswer<DatedRate[]>> {
  const worksheetKey = heldWorksheetKey()
  if (!worksheetKey) return NO_WORKSHEET
  const response = await send('/v1/worksheet/rates', 'GET', worksheetKey)
  return answerFrom(response, async (answered) => (await answered.json()).rates)
}

async function keepRateIn(worksheetKey: string | null, draft: DatedRateDraft) {
  if (!worksheetKey) return UNREACHABLE
  const response = await send('/v1/worksheet/rates', 'POST', worksheetKey, draft)
  return answerFrom<DatedRate>(response, (answered) => answered.json())
}

export async function keepRate(draft: DatedRateDraft): Promise<WorksheetAnswer<DatedRate>> {
  const kept = await keepRateIn(heldWorksheetKey() ?? (await startWorksheet()), draft)
  if (kept.kind !== 'no-worksheet') return kept
  return keepRateIn(await startWorksheet(), draft)
}

export async function removeRate(rateId: string): Promise<WorksheetAnswer<null>> {
  const worksheetKey = heldWorksheetKey()
  if (!worksheetKey) return NO_WORKSHEET
  const response = await send(`/v1/worksheet/rates/${encodeURIComponent(rateId)}`, 'DELETE', worksheetKey)
  return answerFrom(response, async () => null)
}

export async function convert(request: ConversionRequest): Promise<WorksheetAnswer<ConversionReceipt>> {
  const worksheetKey = heldWorksheetKey()
  if (!worksheetKey) return NO_WORKSHEET
  const response = await send('/v1/worksheet/conversions', 'POST', worksheetKey, request)
  return answerFrom(response, (answered) => answered.json())
}
