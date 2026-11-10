import { defineConfig, devices } from '@playwright/test'

const port = process.env.E2E_PORT ?? '7171'
const app = `http://127.0.0.1:${port}`

export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  workers: 1,
  retries: process.env.CI ? 1 : 0,
  timeout: 90_000,
  expect: { timeout: 10_000 },
  reporter: [['list'], ['html', { open: 'never', outputFolder: 'playwright-report' }]],
  use: { baseURL: app, trace: 'retain-on-failure' },
  projects: [
    { name: 'before-any-spec', testMatch: /\.setup\.ts$/ },
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
      testMatch: /\.spec\.ts$/,
      dependencies: ['before-any-spec'],
    },
  ],
  webServer: {
    command: './e2e/serve.sh',
    url: `${app}/v1/health`,
    reuseExistingServer: false,
    timeout: 120_000,
    stdout: 'pipe',
    stderr: 'pipe',
  },
})
