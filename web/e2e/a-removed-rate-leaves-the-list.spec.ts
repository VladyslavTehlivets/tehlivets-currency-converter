import { expect, test } from '@playwright/test'

import { EUR_TO_PLN, holdInTheBrowser, plantWorksheet, PLN_TO_USD, SCREENS } from './worksheet'

for (const screen of SCREENS) {
  test.describe(screen.name, () => {
    test.use({ viewport: screen.viewport })

    test('a removed rate leaves the list', async ({ page, request }) => {
      const { worksheetKey } = await plantWorksheet(request, [EUR_TO_PLN, PLN_TO_USD])
      await holdInTheBrowser(page, worksheetKey)

      await page.goto('/rates')
      const rateTable = page.getByRole('table', { name: 'Your dated rates' })
      const rateRows = rateTable.getByRole('row').filter({ has: page.getByRole('cell') })
      await expect(rateRows).toHaveCount(2)
      await expect(rateRows.filter({ hasText: 'EUR → PLN' })).toContainText('4.2515')
      await expect(rateRows.filter({ hasText: 'PLN → USD' })).toContainText('0.2731')

      await page.getByRole('button', { name: 'Remove EUR → PLN at 2 Mar 2026, 12:00' }).click()

      await expect(rateRows).toHaveCount(1)
      await expect(rateRows.first()).toContainText('PLN → USD')
      await expect(rateTable).not.toContainText('EUR → PLN')
      await expect(rateTable).toBeFocused()

      const listed = await request.get('/v1/worksheet/rates', { headers: { Authorization: `Bearer ${worksheetKey}` } })
      const { rates } = await listed.json()
      expect(rates.map((rate: { from: string; to: string }) => `${rate.from}→${rate.to}`)).toEqual(['PLN→USD'])

      await page.reload()
      await expect(rateRows).toHaveCount(1)
      await expect(rateTable).not.toContainText('EUR → PLN')

      await page.getByRole('button', { name: 'Remove PLN → USD at 2 Mar 2026, 12:00' }).click()
      await expect(page.getByRole('heading', { name: 'Start with a rate.' })).toBeVisible()
    })
  })
}
