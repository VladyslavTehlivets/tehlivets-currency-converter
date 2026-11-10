import { expect, test } from '@playwright/test'

import { EUR_TO_PLN, holdInTheBrowser, plantWorksheet, resultRoute, SCREENS } from './worksheet'

for (const screen of SCREENS) {
  test.describe(screen.name, () => {
    test.use({ viewport: screen.viewport })

    test('a missing direction offers to add exactly that rate', async ({ page, request }) => {
      const { worksheetKey } = await plantWorksheet(request, [EUR_TO_PLN])
      await holdInTheBrowser(page, worksheetKey)

      await page.goto(resultRoute('100', 'EUR', 'USD', '2026-04-20T08:15:00Z'))

      await expect(page.getByRole('heading', { name: 'One step needs a rate.' })).toBeVisible()
      await expect(page.getByRole('heading', { name: 'No result yet.' })).toBeVisible()
      await expect(page.getByText('Missing direction: PLN → USD', { exact: true })).toBeVisible()

      await page.getByRole('link', { name: 'Add the missing rate' }).click()

      await expect(page).toHaveURL(/\/rates\/new\/?\?/)
      const asked = new URL(page.url()).searchParams
      expect(asked.get('from')).toBe('PLN')
      expect(asked.get('to')).toBe('USD')
      const rateForm = page.getByRole('form', { name: 'Your dated rate' })
      await expect(rateForm.getByRole('combobox', { name: 'From' })).toHaveValue('PLN')
      await expect(rateForm.getByRole('combobox', { name: 'To' })).toHaveValue('USD')
      await expect(rateForm.getByLabel('Rate date')).toHaveValue('2026-04-20T08:15')
      await expect(rateForm.getByRole('textbox', { name: 'Rate', exact: true })).toHaveValue('')

      await rateForm.getByRole('textbox', { name: 'Rate', exact: true }).fill('0.2731')
      await rateForm.getByRole('button', { name: 'Keep rate' }).click()
      await expect(page).toHaveURL(/\/rates\/?$/)

      await page.goto(resultRoute('100', 'EUR', 'USD', '2026-04-20T08:15:00Z'))
      await expect(page.getByRole('list', { name: 'Conversion path' }).getByRole('listitem')).toHaveText([
        '100 EUR',
        '→425.15 PLN',
        '→116.108465 USD',
      ])
    })
  })
}
