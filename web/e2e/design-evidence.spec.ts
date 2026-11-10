import { mkdirSync } from 'node:fs'
import { join } from 'node:path'
import { homedir } from 'node:os'
import { test } from '@playwright/test'

import { motionHasSettled, visitEachScreen } from './nine-screens'
import { SCREENS } from './worksheet'

const EVIDENCE_DIR =
  process.env.EVIDENCE_DIR ??
  join(homedir(), '.local/share/orchestrator/evidence/tehlivets-currency-converter-1/screenshots')

for (const screen of SCREENS) {
  test(`the nine design screens are captured at ${screen.viewport.width} px`, async ({ browser, request }) => {
    mkdirSync(EVIDENCE_DIR, { recursive: true })

    await visitEachScreen(browser, request, { viewport: screen.viewport }, async (page, state) => {
      await motionHasSettled(page)
      await page.screenshot({
        path: join(EVIDENCE_DIR, `${state.name}-${screen.viewport.width}.png`),
        fullPage: true,
        animations: 'disabled',
      })
    })
  })
}
