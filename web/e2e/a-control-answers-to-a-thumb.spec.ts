import { expect, test, type Page } from '@playwright/test'

import { motionHasSettled, visitEachScreen } from './nine-screens'
import { SCREENS } from './worksheet'

const THUMB_PX = 44
const ZOOM_THRESHOLD_PX = 16
const PHONE_MENU_BREAKPOINT_PX = 768

async function whatAThumbCannotLandOn(page: Page, where: string): Promise<string[]> {
  const walk = await page.evaluate(
    async ({ thumb, zoomThreshold }) => {
      const half = thumb / 2 - 1
      const settle = () => new Promise((done) => requestAnimationFrame(() => requestAnimationFrame(done)))

      const nameOf = (control: Element) => {
        const spoken =
          control.getAttribute('aria-label') ??
          (control.id ? document.querySelector(`label[for="${control.id}"]`)?.textContent : null) ??
          (control.textContent ?? '').trim().slice(0, 40)
        return spoken && spoken.length > 0 ? spoken : `(unlabelled ${control.tagName.toLowerCase()})`
      }

      const takesATap = (control: Element) => {
        if (control.hasAttribute('disabled') || control.getAttribute('aria-disabled') === 'true') return false
        const box = control.getBoundingClientRect()
        if (box.width === 0 || box.height === 0) return false
        const style = getComputedStyle(control)
        if (style.visibility === 'hidden' || style.pointerEvents === 'none') return false
        return style.display !== 'inline'
      }

      const answersAt = (control: Element, x: number, y: number) => {
        const pastTheEdge = x < 1 || y < 1 || x > window.innerWidth - 1 || y > window.innerHeight - 1
        if (pastTheEdge) return true
        const hit = document.elementFromPoint(x, y)
        return hit !== null && (hit === control || control.contains(hit))
      }

      const unreachable: string[] = []
      let read = 0

      for (const control of Array.from(document.querySelectorAll('button, [role="button"], a[href], input, select, textarea'))) {
        if (!takesATap(control)) continue
        control.scrollIntoView({ block: 'center' })
        await settle()
        if (!takesATap(control)) continue
        read += 1

        const box = control.getBoundingClientRect()
        const isAField = control.matches('input, select, textarea')
        if (isAField && box.height < thumb) unreachable.push(`${nameOf(control)} is ${box.height}px tall`)
        if (isAField && parseFloat(getComputedStyle(control).fontSize) < zoomThreshold) {
          unreachable.push(`${nameOf(control)} zooms the page on focus`)
        }

        const x = box.left + box.width / 2
        const y = box.top + box.height / 2
        const missing = [
          { side: 'above', x, y: y - half },
          { side: 'below', x, y: y + half },
          { side: 'left', x: x - half, y },
          { side: 'right', x: x + half, y },
        ]
          .filter((point) => !answersAt(control, point.x, point.y))
          .map((point) => point.side)

        if (missing.length > 0) unreachable.push(`${nameOf(control)} has no answer ${missing.join(', ')}`)
      }

      window.scrollTo(0, 0)
      return { unreachable, read }
    },
    { thumb: THUMB_PX, zoomThreshold: ZOOM_THRESHOLD_PX },
  )

  expect(walk.read, `${where} showed no control at all, so the walk proves nothing there`).toBeGreaterThan(0)
  return walk.unreachable.map((reading) => `${where}: ${reading}`)
}

for (const screen of SCREENS) {
  test.describe(screen.name, () => {
    test('a control answers to a thumb', async ({ browser, request }) => {
      const unreachable: string[] = []

      await visitEachScreen(browser, request, { viewport: screen.viewport }, async (page, state) => {
        await motionHasSettled(page)
        unreachable.push(...(await whatAThumbCannotLandOn(page, state.name)))

        if (state.name === 'converter' && screen.viewport.width < PHONE_MENU_BREAKPOINT_PX) {
          await page.getByRole('button', { name: 'Menu' }).click()
          await expect(page.getByRole('navigation', { name: 'Primary' })).toBeVisible()
          unreachable.push(...(await whatAThumbCannotLandOn(page, 'converter with the menu open')))
        }
      })

      expect(unreachable, 'controls a thumb cannot land on').toEqual([])
    })
  })
}
