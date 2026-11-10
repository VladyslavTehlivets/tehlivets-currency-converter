import { expect, test, type Locator, type Page } from '@playwright/test'

import { AT_THE_RATES, EUR_TO_PLN, holdInTheBrowser, plantWorksheet, PLN_TO_USD, resultRoute, SCREENS } from './worksheet'

const AMOUNT_COPY = 'Enter a positive number with a dot for decimals, like 12.50.'

const NEAR_PX = 48

async function expectExplainedBesideTheAmount(page: Page) {
  await expect(page.getByRole('heading', { name: 'This amount needs another look.' })).toBeVisible()
  const amount = page.getByRole('textbox', { name: 'Amount' })
  await expect(amount).toHaveAttribute('aria-invalid', 'true')
  await expect(amount).toHaveAccessibleDescription(AMOUNT_COPY)
  await expect(amount).toBeFocused()
  const explanation = page.getByText(AMOUNT_COPY, { exact: true })
  await expect(explanation).toHaveCount(1)
  await expectDirectlyBelow(explanation, amount)
  await expect(page.getByRole('combobox', { name: 'From' })).not.toHaveAttribute('aria-invalid', 'true')
}

async function expectDirectlyBelow(explanation: Locator, field: Locator) {
  const fieldBox = await field.boundingBox()
  const explanationBox = await explanation.boundingBox()
  if (!fieldBox || !explanationBox) throw new Error('the amount field or its explanation has no box')
  const gap = explanationBox.y - (fieldBox.y + fieldBox.height)
  expect(gap, 'the explanation sits under the amount field').toBeGreaterThanOrEqual(0)
  expect(gap, 'the explanation sits right beside the amount field').toBeLessThanOrEqual(NEAR_PX)
  expect(Math.abs(explanationBox.x - fieldBox.x), 'the explanation lines up with the field').toBeLessThanOrEqual(NEAR_PX)
}

for (const screen of SCREENS) {
  test.describe(screen.name, () => {
    test.use({ viewport: screen.viewport })

    test('an invalid amount is explained beside the field', async ({ page, request }) => {
      const { worksheetKey } = await plantWorksheet(request, [EUR_TO_PLN, PLN_TO_USD])
      await holdInTheBrowser(page, worksheetKey)
      const conversionsSent: string[] = []
      page.on('request', (sent) => {
        if (sent.url().includes('/v1/worksheet/conversions')) conversionsSent.push(sent.url())
      })

      await page.goto('/')
      await page.getByRole('textbox', { name: 'Amount' }).fill('12,50')
      await page.getByRole('button', { name: 'Convert amount' }).click()

      await expectExplainedBesideTheAmount(page)
      await expect(page).toHaveURL(/\/$/)
      expect(conversionsSent, 'the client check stops the request').toEqual([])

      await page.goto(resultRoute('0', 'EUR', 'USD', AT_THE_RATES))

      await expect(page).toHaveURL(/refusal=invalid_amount/)
      await expectExplainedBesideTheAmount(page)
      await expect(page.getByRole('textbox', { name: 'Amount' })).toHaveValue('0')
      expect(conversionsSent, 'the server refused the amount').toHaveLength(1)

      await page.getByRole('textbox', { name: 'Amount' }).fill('12.50')
      await page.getByRole('button', { name: 'Convert amount' }).click()
      await expect(page.getByRole('heading', { name: 'Here is how the amount moves.' })).toBeVisible()
    })
  })
}
