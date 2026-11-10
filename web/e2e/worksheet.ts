import type { APIRequestContext, Page } from '@playwright/test'

const WORKSHEET_KEY_ENTRY = 'tehlivets-currency-converter.worksheet-key'

export const SCREENS = [
  { name: 'phone', viewport: { width: 390, height: 844 } },
  { name: 'tablet', viewport: { width: 768, height: 1024 } },
  { name: 'laptop', viewport: { width: 1440, height: 900 } },
] as const

export interface PlantedRate {
  from: string
  to: string
  rate: string
  ratedAt: string
}

export interface PlantedWorksheet {
  worksheetKey: string
  rateIds: string[]
}

export async function plantWorksheet(request: APIRequestContext, rates: PlantedRate[]): Promise<PlantedWorksheet> {
  const started = await request.post('/v1/worksheets')
  const { worksheetKey } = await started.json()
  const rateIds: string[] = []
  for (const rate of rates) {
    const kept = await request.post('/v1/worksheet/rates', {
      headers: { Authorization: `Bearer ${worksheetKey}` },
      data: rate,
    })
    if (kept.status() !== 201) throw new Error(`planting ${JSON.stringify(rate)} answered ${kept.status()}`)
    rateIds.push((await kept.json()).id)
  }
  return { worksheetKey, rateIds }
}

export async function holdInTheBrowser(page: Page, worksheetKey: string): Promise<void> {
  await page.addInitScript(
    ([entry, key]) => {
      if (!sessionStorage.getItem('planted')) {
        localStorage.setItem(entry, key)
        sessionStorage.setItem('planted', 'yes')
      }
    },
    [WORKSHEET_KEY_ENTRY, worksheetKey],
  )
}

export async function heldWorksheetKey(page: Page): Promise<string | null> {
  return page.evaluate((entry) => localStorage.getItem(entry), WORKSHEET_KEY_ENTRY)
}

export const EUR_TO_PLN: PlantedRate = { from: 'EUR', to: 'PLN', rate: '4.2515', ratedAt: '2026-03-02T12:00:00Z' }
export const PLN_TO_USD: PlantedRate = { from: 'PLN', to: 'USD', rate: '0.2731', ratedAt: '2026-03-02T12:00:00Z' }
export const AT_THE_RATES = '2026-03-02T12:00:00Z'

export function resultRoute(amount: string, from: string, to: string, at = AT_THE_RATES): string {
  return `/result?${new URLSearchParams({ amount, from, to, at })}`
}

export async function widthsOf(page: Page) {
  return page.evaluate(() => ({ scrolled: document.documentElement.scrollWidth, visible: window.innerWidth }))
}
