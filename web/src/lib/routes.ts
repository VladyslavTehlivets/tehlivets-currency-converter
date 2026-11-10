import type { ConversionRequest, MissingDirection, Refusal } from '@/lib/worksheet-api'

export const CONVERT_ROUTE = '/'
export const RESULT_ROUTE = '/result'
export const RATES_ROUTE = '/rates'
export const RATE_ADD_ROUTE = '/rates/new'
export const METHOD_ROUTE = '/how-it-works'

const withQuery = (route: string, query: Record<string, string>) => {
  const written = new URLSearchParams(query).toString()
  return written ? `${route}?${written}` : route
}

export function resultHref(request: ConversionRequest): string {
  return withQuery(RESULT_ROUTE, { ...request })
}

export function converterHref(request: ConversionRequest, refusal?: Pick<Refusal, 'code' | 'field'>): string {
  return withQuery(CONVERT_ROUTE, {
    ...request,
    ...(refusal ? { refusal: refusal.code } : {}),
    ...(refusal?.field ? { field: refusal.field } : {}),
  })
}

export function rateAddHref(missing: MissingDirection, at: string): string {
  return withQuery(RATE_ADD_ROUTE, { from: missing.from, to: missing.to, at })
}

export function conversionRequestIn(query: URLSearchParams): ConversionRequest {
  return {
    amount: query.get('amount') ?? '',
    from: query.get('from') ?? '',
    to: query.get('to') ?? '',
    at: query.get('at') ?? '',
  }
}

export function refusalsIn(query: URLSearchParams): Refusal[] {
  const code = query.get('refusal')
  const field = query.get('field')
  if (!code) return []
  return [{ code, ...(field ? { field } : {}) }]
}
