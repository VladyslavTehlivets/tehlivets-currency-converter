import { expect, test, type Page } from '@playwright/test'

import { motionHasSettled, NINE_SCREENS, openTheConverter, visitEachScreen } from './nine-screens'
import { convertedAmountOn, EUR_TO_PLN, holdInTheBrowser, plantWorksheet, PLN_TO_USD, SCREENS } from './worksheet'

const MOST_TAB_STOPS = 60
const PHONE_MENU_BREAKPOINT_PX = 768

interface TabStop {
  control: string | null
  name: string
  focusIsVisible: boolean
  inView: boolean
}

async function markTheVisibleControls(page: Page): Promise<string[]> {
  return page.evaluate(() => {
    const isShown = (control: Element) => {
      const box = control.getBoundingClientRect()
      const style = getComputedStyle(control)
      return box.width > 0 && box.height > 0 && style.visibility !== 'hidden' && !control.closest('[aria-hidden="true"]')
    }
    const controls = Array.from(
      document.querySelectorAll('a[href], button:not([disabled]), input:not([type="hidden"]), select, textarea'),
    )
    return controls.filter(isShown).map((control, index) => {
      const mark = `control-${index}`
      control.setAttribute('data-e2e-control', mark)
      return mark
    })
  })
}

async function readTheFocusedControl(page: Page): Promise<TabStop> {
  return page.evaluate(async () => {
    const nextFrame = () => new Promise((done) => requestAnimationFrame(done))
    let lastScroll = -1
    while (window.scrollY !== lastScroll) {
      lastScroll = window.scrollY
      await nextFrame()
      await nextFrame()
    }
    const focused = document.activeElement
    if (!focused || focused === document.body) return { control: null, name: 'body', focusIsVisible: false, inView: false }
    const style = getComputedStyle(focused)
    const outlineIsDrawn =
      style.outlineStyle !== 'none' && parseFloat(style.outlineWidth) >= 2 && style.outlineColor !== 'rgba(0, 0, 0, 0)'
    const box = focused.getBoundingClientRect()
    return {
      control: focused.getAttribute('data-e2e-control'),
      name:
        focused.getAttribute('aria-label') ??
        (focused.id ? document.querySelector(`label[for="${focused.id}"]`)?.textContent : null) ??
        (focused.textContent ?? '').trim().slice(0, 40),
      focusIsVisible: focused.matches(':focus-visible') && outlineIsDrawn,
      inView: box.bottom > 0 && box.top < window.innerHeight && box.right > 0 && box.left < window.innerWidth,
    }
  })
}

async function tabThroughThePage(page: Page): Promise<TabStop[]> {
  await page.evaluate(() => {
    const top = document.body
    top.setAttribute('tabindex', '-1')
    top.focus({ preventScroll: true })
    top.removeAttribute('tabindex')
    top.blur()
  })
  const stops: TabStop[] = []
  for (let press = 0; press < MOST_TAB_STOPS; press += 1) {
    await page.keyboard.press('Tab')
    const stop = await readTheFocusedControl(page)
    if (stop.control === null && stop.name === 'body') break
    const stillOnTheSameControl = stops.at(-1)?.control === stop.control && stop.control !== null
    if (stillOnTheSameControl) continue
    if (stops.some((seen) => seen.control !== null && seen.control === stop.control)) break
    stops.push(stop)
  }
  return stops
}

async function whatTheKeyboardMisses(page: Page, where: string): Promise<string[]> {
  const visibleControls = await markTheVisibleControls(page)
  expect(visibleControls.length, `${where} shows no control, so the walk proves nothing`).toBeGreaterThan(0)
  const stops = await tabThroughThePage(page)
  const reached = new Set(stops.map((stop) => stop.control))
  return [
    ...visibleControls.filter((control) => !reached.has(control)).map((control) => `${where}: ${control} is never reached`),
    ...stops.filter((stop) => !stop.focusIsVisible).map((stop) => `${where}: "${stop.name}" takes focus without a visible ring`),
    ...stops.filter((stop) => !stop.inView).map((stop) => `${where}: "${stop.name}" takes focus out of view`),
  ]
}

for (const screen of SCREENS) {
  test.describe(screen.name, () => {
    test.use({ viewport: screen.viewport })

    test('every action is reachable by keyboard with visible focus', async ({ browser, page, request }) => {
      const missed: string[] = []
      const phoneMenu = screen.viewport.width < PHONE_MENU_BREAKPOINT_PX

      await visitEachScreen(browser, request, { viewport: screen.viewport }, async (visited, state) => {
        await motionHasSettled(visited)
        missed.push(...(await whatTheKeyboardMisses(visited, state.name)))
      }, NINE_SCREENS)

      expect(missed, 'actions the keyboard cannot reach or shows without focus').toEqual([])

      const { worksheetKey } = await plantWorksheet(request, [EUR_TO_PLN, PLN_TO_USD])
      await holdInTheBrowser(page, worksheetKey)
      await openTheConverter(page)

      if (phoneMenu) {
        const menu = page.getByRole('button', { name: 'Menu' })
        await menu.focus()
        await page.keyboard.press('Enter')
        await expect(menu).toHaveAttribute('aria-expanded', 'true')
        await page.keyboard.press('Tab')
        await expect(page.getByRole('navigation', { name: 'Primary' }).getByRole('link', { name: 'Convert' })).toBeFocused()
        await page.keyboard.press('Escape')
        await expect(menu).toHaveAttribute('aria-expanded', 'false')
        await expect(menu).toBeFocused()
      }

      const amount = page.getByRole('textbox', { name: 'Amount' })
      await amount.focus()
      await page.keyboard.type('100')
      await page.keyboard.press('Tab')
      await expect(page.getByRole('combobox', { name: 'From' })).toBeFocused()
      await page.keyboard.press('e')
      await expect(page.getByRole('combobox', { name: 'From' })).toHaveValue('EUR')
      await page.keyboard.press('Tab')
      await expect(page.getByRole('combobox', { name: 'To' })).toBeFocused()
      await page.keyboard.press('u')
      await expect(page.getByRole('combobox', { name: 'To' })).toHaveValue('USD')
      await page.keyboard.press('Tab')
      await expect(page.getByLabel('Conversion date')).toBeFocused()
      const convertAmount = page.getByRole('button', { name: 'Convert amount' })
      await expect(async () => {
        await page.keyboard.press('Tab')
        await expect(convertAmount).toBeFocused({ timeout: 100 })
      }).toPass({ timeout: 5_000 })
      await page.keyboard.press('Enter')
      await expect(convertedAmountOn(page)).toHaveText('116.108465 USD')

      await page.goto('/rates')
      const removeEurToPln = page.getByRole('button', { name: 'Remove EUR → PLN at 2 Mar 2026, 12:00' })
      await removeEurToPln.focus()
      await page.keyboard.press('Enter')
      await expect(removeEurToPln).toHaveCount(0)
      await expect(page.getByRole('table', { name: 'Your dated rates' })).toBeFocused()
    })
  })
}
