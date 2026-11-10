import { expect, test } from '@playwright/test'

import { holdTheConversionInFlight } from './nine-screens'
import { convertedAmountOn, EUR_TO_PLN, holdInTheBrowser, plantWorksheet, resultRoute, SCREENS } from './worksheet'

for (const screen of SCREENS) {
  test.describe(screen.name, () => {
    test.use({ viewport: screen.viewport })

    test('the result shows its skeleton while the request is in flight', async ({ page, request }) => {
      const { worksheetKey } = await plantWorksheet(request, [EUR_TO_PLN])
      await holdInTheBrowser(page, worksheetKey)
      const conversion = await holdTheConversionInFlight(page)
      const conversionSent = page.waitForRequest('**/v1/worksheet/conversions')

      await page.goto(resultRoute('100', 'EUR', 'PLN'))
      await conversionSent

      const skeleton = page.getByRole('status', { name: 'Loading conversion' })
      await expect(page.getByRole('heading', { name: 'Finding the closest dated rate.' })).toBeVisible()
      await expect(page.getByText('Your amount stays here while the worksheet loads.')).toBeVisible()
      await expect(skeleton).toBeVisible()
      await expect(skeleton.locator('[data-slot="skeleton"]')).toHaveCount(7)
      await expect(page.getByRole('heading', { name: 'Here is how the amount moves.' })).toHaveCount(0)

      conversion.release()

      await expect(convertedAmountOn(page)).toHaveText('425.15 PLN')
      await expect(skeleton).toHaveCount(0)
      await expect(page.getByRole('heading', { name: 'Finding the closest dated rate.' })).toHaveCount(0)
    })
  })
}
