import { expect, test, type Page } from '@playwright/test'
import { mkdirSync } from 'node:fs'
import { join } from 'node:path'

const EVIDENCE = process.env.E2E_EVIDENCE_DIR
const TEST_EMAIL = 'factory-e2e@currency.vtehsol.com'
const TEST_PASSWORD = 'e2e-only-password'

async function shoot(page: Page, name: string) {
  if (!EVIDENCE) return
  mkdirSync(EVIDENCE, { recursive: true })
  await page.screenshot({ path: join(EVIDENCE, `${name}.png`), fullPage: true })
}

const troubleNotice = (page: Page) => page.locator('[role="alert"]:not(#__next-route-announcer__)')

const VIEWPORTS = [
  { name: '390', width: 390, height: 844 },
  { name: '1440', width: 1440, height: 900 },
]

for (const viewport of VIEWPORTS) {
  test.describe(`at ${viewport.name}px`, () => {
    test.use({ viewport: { width: viewport.width, height: viewport.height } })

    test('the Google door is one thumb-sized control and the page does not scroll sideways', async ({ page }) => {
      await page.goto('/sign-in')
      const door = page.getByRole('button', { name: 'Continue with Google' })

      await expect(door).toBeVisible()
      const box = await door.boundingBox()
      expect(box?.height).toBeGreaterThanOrEqual(44)
      expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true)
      await shoot(page, `sign-in-idle-${viewport.name}`)
    })

    test('a refusal code shows closed copy, never the code, and leaves the address clean', async ({ page }) => {
      await page.goto('/sign-in?refused=email-unverified&door=google&next=%2Frates')

      const notice = troubleNotice(page)
      await expect(notice).toContainText("Google has not verified this account's email")
      await expect(notice).not.toContainText('email-unverified')
      await expect(page).toHaveURL(/\/sign-in\?next=%2Frates$/)
      await shoot(page, `sign-in-refused-${viewport.name}`)
    })
  })
}

test('cancelling at Google reads as neutral status copy', async ({ page }) => {
  await page.goto('/sign-in?refused=cancelled&door=google')

  await expect(page.getByRole('status').filter({ hasText: 'You cancelled at Google' })).toBeVisible()
})

test('an unknown refusal code says the sign-in did not finish', async ({ page }) => {
  await page.goto('/sign-in?refused=%3Cb%3Ex%3C%2Fb%3E')

  await expect(troubleNotice(page)).toContainText('That sign-in did not finish')
})

test('pressing the door where Google is not set up returns with the provider unavailable notice', async ({ page }) => {
  await page.goto('/sign-in?next=%2Frates')

  await page.getByRole('button', { name: 'Continue with Google' }).click()

  await expect(troubleNotice(page)).toContainText('Google did not answer')
  await expect(page).toHaveURL(/next=%2Frates/)
})

test('the footer offers sign in from every route and carries the current path', async ({ page }) => {
  await page.goto('/rates')

  await page.getByRole('contentinfo').getByRole('link', { name: 'Sign in' }).click()

  await expect(page).toHaveURL(/\/sign-in\?next=%2Frates$/)
})

test('the dev test door signs in only where the dev flag is set', async ({ page, context }) => {
  await page.goto('/sign-in?next=%2Frates')
  await expect(page.getByText('Dev only · test door')).toBeVisible()
  await shoot(page, 'sign-in-test-door')

  await page.getByLabel('Test email').fill(TEST_EMAIL)
  await page.getByLabel('Test password').fill(TEST_PASSWORD)
  await page.getByRole('button', { name: 'Sign in to dev' }).click()

  await expect(page).toHaveURL(/\/rates$/)
  const session = (await context.cookies()).find((cookie) => cookie.name === '__Host-session')
  expect(session?.httpOnly).toBe(true)
  expect(session?.secure).toBe(true)
})

test('a wrong test door password is refused', async ({ page }) => {
  await page.goto('/sign-in')
  await page.getByLabel('Test email').fill(TEST_EMAIL)
  await page.getByLabel('Test password').fill('wrong')
  await page.getByRole('button', { name: 'Sign in to dev' }).click()

  await expect(troubleNotice(page)).toContainText('This account is not invited')
})

test('production serves no password form', async ({ page }) => {
  await page.route('**/auth/test/door', (route) => route.fulfill({ status: 404 }))

  await page.goto('/sign-in')

  await expect(page.getByRole('button', { name: 'Continue with Google' })).toBeVisible()
  await expect(page.getByLabel('Test password')).toHaveCount(0)
  await expect(page.locator('input[type="password"]')).toHaveCount(0)
})
