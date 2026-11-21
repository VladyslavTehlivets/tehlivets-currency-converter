import { expect, test } from '@playwright/test'

import { SCREENS } from './worksheet'

const REFUSAL_TITLES: Record<string, string> = {
  cancelled: 'You cancelled at Google',
  'state-mismatch': 'That sign-in did not finish',
  'token-for-another-client': 'That sign-in did not finish',
  'email-unverified': "Google has not verified this account's email",
  'not-invited': 'This Google account cannot sign in here',
  'provider-unavailable': 'Google is not answering',
}

for (const screen of SCREENS) {
  test.describe(screen.name, () => {
    test.use({ viewport: screen.viewport })

    test('the Google door is one full width control at least 44px tall', async ({ page }) => {
      await page.goto('/sign-in')
      const door = page.getByRole('button', { name: 'Continue with Google' })
      const box = await door.boundingBox()

      expect(box?.height).toBeGreaterThanOrEqual(44)
      expect(box?.width).toBeGreaterThan(200)
      await expect(page.getByRole('heading', { level: 1 })).toHaveText('Sign in to Currency Converter.')
      await expect(page.locator('form[action="/auth/google/start"] input[name=next]')).toHaveValue('/')
    })

    test('the Google mark is decorative and the door paints Google values only', async ({ page }) => {
      await page.goto('/sign-in')
      const door = page.getByRole('button', { name: 'Continue with Google' })

      await expect(door.locator('img')).toHaveAttribute('alt', '')
      await expect(door).toHaveCSS('background-color', 'rgb(255, 255, 255)')
      await expect(door).toHaveCSS('color', 'rgb(31, 31, 31)')
      await expect(door).toHaveCSS('border-top-color', 'rgb(116, 119, 117)')
    })

    test('production serves no password form and no test door', async ({ page, request }) => {
      await page.goto('/sign-in')
      await expect(page.getByRole('button', { name: 'Continue with Google' })).toBeVisible()

      await expect(page.locator('input[type=password]')).toHaveCount(0)
      await expect(page.getByText('Dev only')).toHaveCount(0)
      await expect(page.getByLabel('Test member email')).toHaveCount(0)
      const door = await request.post('/auth/test-door', { data: { email: 'a@b.c', password: 'x' } })
      expect(door.status()).toBe(404)
    })

    for (const [code, title] of Object.entries(REFUSAL_TITLES)) {
      test(`a ${code} return shows its notice, takes focus, and leaves no code in the address`, async ({ page }) => {
        await page.goto(`/sign-in?refused=${code}&door=google`)
        const notice = page.getByText(title, { exact: true })

        await expect(notice).toBeVisible()
        await expect(page.locator('[tabindex="-1"]', { hasText: title })).toBeFocused()
        await expect(page).toHaveURL(/\/sign-in$/)
        await expect(page.getByRole('button', { name: 'Continue with Google' })).toBeEnabled()
      })
    }

    test('free text in a return address never reaches the page', async ({ page }) => {
      await page.goto('/sign-in?refused=ada%40example.com&door=google')

      await expect(page.getByText('That sign-in did not finish')).toBeVisible()
      await expect(page.getByText('ada@example.com')).toHaveCount(0)
    })

    test('the door goes busy once and a hostile next path falls back home', async ({ page }) => {
      await page.goto('/sign-in?next=//evil.example')
      await expect(page.locator('input[name=next]')).toHaveValue('/')
      await page.route('**/auth/google/start**', () => {})

      await page.getByRole('button', { name: 'Continue with Google' }).click()

      const busy = page.getByRole('button', { name: 'Opening Google…' })
      await expect(busy).toBeDisabled()
      await expect(busy).toHaveAttribute('aria-busy', 'true')
    })

    test('the Sign in link is in the navigation and marks this page', async ({ page }) => {
      await page.goto('/')
      const viewport = page.viewportSize()
      if (viewport && viewport.width < 768) await page.getByRole('button', { name: 'Menu' }).click()
      await page.getByRole('link', { name: 'Sign in' }).first().click()

      await expect(page).toHaveURL(/\/sign-in$/)
    })
  })
}
