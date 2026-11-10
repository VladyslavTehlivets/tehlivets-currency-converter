import { execSync } from 'node:child_process'
import { expect, test } from '@playwright/test'

test('the served build is this checkout', async ({ request }) => {
  const health = await (await request.get('/v1/health')).json()
  expect(health.revision).toBe(execSync('git rev-parse HEAD').toString().trim())
  const home = await request.get('/')
  expect(home.status()).toBe(200)
})
