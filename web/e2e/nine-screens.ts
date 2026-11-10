import { expect, test, type APIRequestContext, type Browser, type BrowserContextOptions, type Page } from '@playwright/test'

import { EUR_TO_PLN, holdInTheBrowser, plantWorksheet, PLN_TO_USD, resultRoute } from './worksheet'

export interface ScreenState {
  name: string
  open: (page: Page, request: APIRequestContext) => Promise<void>
}

const CONVERSIONS = '**/v1/worksheet/conversions'

export interface HeldConversion {
  release: () => void
}

export async function holdTheConversionInFlight(page: Page): Promise<HeldConversion> {
  let release: () => void = () => {}
  const released = new Promise<void>((resolve) => {
    release = resolve
  })
  page.once('close', () => release())
  await page.route(CONVERSIONS, async (route) => {
    await released
    await route.continue().catch(() => {})
  })
  return { release }
}

async function holdBothRates(page: Page, request: APIRequestContext) {
  const { worksheetKey } = await plantWorksheet(request, [EUR_TO_PLN, PLN_TO_USD])
  await holdInTheBrowser(page, worksheetKey)
}

async function holdOnlyEurToPln(page: Page, request: APIRequestContext) {
  const { worksheetKey } = await plantWorksheet(request, [EUR_TO_PLN])
  await holdInTheBrowser(page, worksheetKey)
}

export async function openTheConverter(page: Page) {
  await page.goto('/')
  await expect(page.getByRole('textbox', { name: 'Amount' })).toBeVisible()
}

export const NINE_SCREENS: ScreenState[] = [
  {
    name: 'converter',
    open: async (page, request) => {
      await holdBothRates(page, request)
      await openTheConverter(page)
      await expect(page.getByRole('heading', { level: 1 })).toHaveText(/Every amount\.\s*A visible path\./)
    },
  },
  {
    name: 'loading',
    open: async (page, request) => {
      await holdBothRates(page, request)
      await holdTheConversionInFlight(page)
      await page.goto(resultRoute('100', 'EUR', 'USD'))
      await expect(page.getByRole('heading', { name: 'Finding the closest dated rate.' })).toBeVisible()
      await expect(page.getByRole('status', { name: 'Loading conversion' })).toBeVisible()
    },
  },
  {
    name: 'result',
    open: async (page, request) => {
      await holdBothRates(page, request)
      await page.goto(resultRoute('100', 'EUR', 'USD'))
      await expect(page.getByRole('heading', { name: 'Here is how the amount moves.' })).toBeVisible()
    },
  },
  {
    name: 'missing-rate',
    open: async (page, request) => {
      await holdOnlyEurToPln(page, request)
      await page.goto(resultRoute('100', 'EUR', 'USD'))
      await expect(page.getByRole('heading', { name: 'One step needs a rate.' })).toBeVisible()
    },
  },
  {
    name: 'error',
    open: async (page, request) => {
      await holdBothRates(page, request)
      await openTheConverter(page)
      await page.getByRole('textbox', { name: 'Amount' }).fill('12,50')
      await page.getByRole('button', { name: 'Convert amount' }).click()
      await expect(page.getByRole('heading', { name: 'This amount needs another look.' })).toBeVisible()
    },
  },
  {
    name: 'rates',
    open: async (page, request) => {
      await holdBothRates(page, request)
      await page.goto('/rates')
      await expect(page.getByRole('table', { name: 'Your dated rates' })).toBeVisible()
    },
  },
  {
    name: 'empty',
    open: async (page) => {
      await page.goto('/rates')
      await expect(page.getByRole('heading', { name: 'Start with a rate.' })).toBeVisible()
    },
  },
  {
    name: 'rate-add',
    open: async (page) => {
      await page.goto('/rates/new')
      await expect(page.getByRole('heading', { name: 'Keep the rate with its date.' })).toBeVisible()
    },
  },
  {
    name: 'method',
    open: async (page) => {
      await page.goto('/how-it-works')
      await expect(page.getByRole('heading', { name: 'A result you can follow.' })).toBeVisible()
    },
  },
]

export async function motionHasSettled(page: Page) {
  await page.evaluate(() =>
    Promise.all(
      document
        .getAnimations()
        .filter((animation) => animation.effect?.getComputedTiming().iterations !== Infinity)
        .map((animation) => animation.finished.catch(() => null)),
    ),
  )
}

export async function visitEachScreen(
  browser: Browser,
  request: APIRequestContext,
  options: BrowserContextOptions,
  visit: (page: Page, screen: ScreenState) => Promise<void>,
  screens: ScreenState[] = NINE_SCREENS,
) {
  const { baseURL, timezoneId } = test.info().project.use
  for (const screen of screens) {
    const context = await browser.newContext({ baseURL, timezoneId, ...options })
    try {
      const page = await context.newPage()
      await screen.open(page, request)
      await visit(page, screen)
    } finally {
      await context.close()
    }
  }
}
