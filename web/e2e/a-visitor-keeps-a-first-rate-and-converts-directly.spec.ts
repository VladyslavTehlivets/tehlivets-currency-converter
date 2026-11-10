import { expect, test } from '@playwright/test'

import { convertedAmountOn, heldWorksheetKey, SCREENS } from './worksheet'

for (const screen of SCREENS) {
  test.describe(screen.name, () => {
    test.use({ viewport: screen.viewport })

    test('a visitor keeps a first rate and converts directly', async ({ page }) => {
      await page.goto('/rates/new')
      expect(await heldWorksheetKey(page)).toBeNull()

      const rateForm = page.getByRole('form', { name: 'Your dated rate' })
      await rateForm.getByRole('combobox', { name: 'From' }).selectOption('EUR')
      await rateForm.getByRole('combobox', { name: 'To' }).selectOption('PLN')
      await rateForm.getByRole('textbox', { name: 'Rate', exact: true }).fill('4.2515')
      await rateForm.getByLabel('Rate date').fill('2026-03-02T12:00')
      await rateForm.getByRole('button', { name: 'Keep rate' }).click()

      await expect(page).toHaveURL(/\/rates\/?$/)
      const rateRows = page.getByRole('table', { name: 'Your dated rates' }).getByRole('row')
      await expect(rateRows).toHaveCount(2)
      await expect(rateRows.nth(1)).toContainText('EUR → PLN')
      await expect(rateRows.nth(1)).toContainText('4.2515')
      await expect(rateRows.nth(1)).toContainText('2 Mar 2026, 12:00')
      expect(await heldWorksheetKey(page)).toEqual(expect.any(String))

      await page.goto('/')
      const conversionForm = page.getByRole('form', { name: 'Your conversion' })
      await conversionForm.getByRole('textbox', { name: 'Amount' }).fill('100')
      await conversionForm.getByRole('combobox', { name: 'From' }).selectOption('EUR')
      await conversionForm.getByRole('combobox', { name: 'To' }).selectOption('PLN')
      await conversionForm.getByLabel('Conversion date').fill('2026-03-02T12:00')
      await conversionForm.getByRole('button', { name: 'Convert amount' }).click()

      await expect(page).toHaveURL(/\/result\/?\?/)
      await expect(page.getByRole('heading', { name: 'Here is how the amount moves.' })).toBeVisible()
      await expect(convertedAmountOn(page)).toHaveText('425.15 PLN')
      await expect(page.getByRole('list', { name: 'Conversion path' }).getByRole('listitem')).toHaveText([
        '100 EUR',
        '→425.15 PLN',
      ])
      const legs = page.getByRole('list', { name: 'Selected rates' }).getByRole('listitem')
      await expect(legs).toHaveCount(1)
      await expect(legs.first()).toContainText('EUR → PLN')
      await expect(legs.first()).toContainText('2 March 2026 · 12:00')
      await expect(legs.first()).toContainText('4.2515')
      await expect(page.getByText('Example rates. This result is not a market quote.')).toBeVisible()
    })
  })
}
