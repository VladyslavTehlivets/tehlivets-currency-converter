import { expect, test } from '@playwright/test'

import { convertedAmountOn, EUR_TO_PLN, holdInTheBrowser, plantWorksheet, PLN_TO_USD, resultRoute, SCREENS } from './worksheet'

const PLN_TO_USD_RATED_LATER = { from: 'PLN', to: 'USD', rate: '0.2899', ratedAt: '2026-06-15T09:30:00Z' }

for (const screen of SCREENS) {
  test.describe(screen.name, () => {
    test.use({ viewport: screen.viewport })

    test('a cross conversion shows two steps and their dated rates', async ({ page, request }) => {
      const { worksheetKey } = await plantWorksheet(request, [EUR_TO_PLN, PLN_TO_USD, PLN_TO_USD_RATED_LATER])
      await holdInTheBrowser(page, worksheetKey)

      await page.goto(resultRoute('100', 'EUR', 'USD', '2026-03-05T00:00:00Z'))

      await expect(page.getByRole('heading', { name: 'Here is how the amount moves.' })).toBeVisible()
      await expect(convertedAmountOn(page)).toHaveText('116.108465 USD')
      await expect(page.getByText('100 EUR · Example rates · 5 March 2026')).toBeVisible()

      await expect(page.getByRole('list', { name: 'Conversion path' }).getByRole('listitem')).toHaveText([
        '100 EUR',
        '→425.15 PLN',
        '→116.108465 USD',
      ])

      const legs = page.getByRole('list', { name: 'Selected rates' }).getByRole('listitem')
      await expect(legs).toHaveCount(2)
      await expect(legs.nth(0)).toContainText('EUR → PLN')
      await expect(legs.nth(0)).toContainText('2 March 2026 · 12:00')
      await expect(legs.nth(0)).toContainText('4.2515')
      await expect(legs.nth(1)).toContainText('PLN → USD')
      await expect(legs.nth(1)).toContainText('2 March 2026 · 12:00')
      await expect(legs.nth(1)).toContainText('0.2731')
      await expect(legs.nth(1)).not.toContainText('0.2899')
    })
  })
}
