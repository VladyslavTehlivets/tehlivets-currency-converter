import { expect, test } from '@playwright/test'

import { motionHasSettled, visitEachScreen } from './nine-screens'
import { SCREENS, widthsOf } from './worksheet'

for (const screen of SCREENS) {
  test.describe(screen.name, () => {
    test('every route fits the screen without sideways scroll', async ({ browser, request }) => {
      const scrollingSideways: string[] = []

      await visitEachScreen(browser, request, { viewport: screen.viewport }, async (page, state) => {
        await motionHasSettled(page)
        const widths = await widthsOf(page)
        expect(widths.visible).toBe(screen.viewport.width)
        if (widths.scrolled > widths.visible) {
          scrollingSideways.push(`${state.name} scrolls ${widths.scrolled - widths.visible}px sideways`)
        }
      })

      expect(scrollingSideways).toEqual([])
    })
  })
}
