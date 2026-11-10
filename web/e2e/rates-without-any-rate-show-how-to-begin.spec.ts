import { expect, test, type Page } from '@playwright/test'

import { holdInTheBrowser, plantWorksheet, resultRoute, SCREENS } from './worksheet'

async function expectHowToBegin(page: Page) {
  await expect(page.getByRole('heading', { name: 'Start with a rate.' })).toBeVisible()
  await expect(page.getByText('Your worksheet has no rates yet.')).toBeVisible()
  await expect(page.getByRole('heading', { name: 'Keep your first dated rate.' })).toBeVisible()
  await expect(page.getByRole('table', { name: 'Your dated rates' })).toHaveCount(0)
}

for (const screen of SCREENS) {
  test.describe(screen.name, () => {
    test.use({ viewport: screen.viewport })

    test('rates without any rate show how to begin', async ({ page, request }) => {
      const worksheetCalls: string[] = []
      page.on('request', (sent) => {
        if (sent.url().includes('/v1/')) worksheetCalls.push(`${sent.method()} ${new URL(sent.url()).pathname}`)
      })

      await page.goto('/rates')
      await expectHowToBegin(page)
      await page.goto(resultRoute('100', 'EUR', 'USD'))
      await expectHowToBegin(page)
      expect(worksheetCalls, 'a visitor without a key sends no worksheet request').toEqual([])

      await page.goto('/rates')
      await page.getByRole('main').getByRole('link', { name: 'Add a rate' }).click()
      await expect(page).toHaveURL(/\/rates\/new\/?$/)
      await expect(page.getByRole('heading', { name: 'Keep the rate with its date.' })).toBeVisible()

      const { worksheetKey } = await plantWorksheet(request, [])
      const holder = await page.context().newPage()
      await holdInTheBrowser(holder, worksheetKey)
      await holder.goto('/rates')
      await expectHowToBegin(holder)
    })
  })
}
