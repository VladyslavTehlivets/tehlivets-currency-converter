import AxeBuilder from '@axe-core/playwright'
import { expect, test } from '@playwright/test'

import { motionHasSettled, visitEachScreen } from './nine-screens'
import { SCREENS } from './worksheet'

for (const screen of SCREENS) {
  test.describe(screen.name, () => {
    test('every route has no axe violations', async ({ browser, request }) => {
      const violations: string[] = []

      await visitEachScreen(browser, request, { viewport: screen.viewport }, async (page, state) => {
        await motionHasSettled(page)
        const report = await new AxeBuilder({ page }).analyze()
        expect(report.passes.length, `${state.name} gave axe nothing to check`).toBeGreaterThan(0)
        for (const violation of report.violations) {
          const targets = violation.nodes.map((node) => node.target.join(' ')).join(' | ')
          violations.push(`${state.name}: ${violation.id} (${violation.impact}) at ${targets}`)
        }
      })

      expect(violations).toEqual([])
    })
  })
}
