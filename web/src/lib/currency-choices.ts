import type { DatedRate } from '@/lib/worksheet-api'

export const BASE_CURRENCY = 'PLN'

const FIRST_CHOICES = [BASE_CURRENCY, 'EUR', 'USD']

const unique = (currencies: string[]) => [...new Set(currencies)]

export function conversionChoices(rates: DatedRate[]): string[] {
  if (rates.length === 0) return FIRST_CHOICES
  const inTheWorksheet = rates.flatMap((rate) => [rate.from, rate.to]).sort()
  return unique([BASE_CURRENCY, ...inTheWorksheet])
}

export function rateChoices(): string[] {
  return unique([...FIRST_CHOICES, ...Intl.supportedValuesOf('currency')])
}

export function chosenPair(
  choices: string[],
  wanted: { from: string | null; to: string | null },
  preferred: { from: string; to: string },
): { from: string; to: string } {
  const offered = (currency: string | null): currency is string => currency !== null && choices.includes(currency)
  const from = [wanted.from, preferred.from, choices[0]].find(offered) ?? BASE_CURRENCY
  const to =
    [wanted.to].find(offered) ??
    [preferred.to, ...choices].find((currency) => offered(currency) && currency !== from) ??
    from
  return { from, to }
}
