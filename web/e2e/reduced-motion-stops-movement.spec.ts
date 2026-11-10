import { expect, test, type Page } from '@playwright/test'

import { NINE_SCREENS, visitEachScreen } from './nine-screens'
import { SCREENS } from './worksheet'

const EFFECTIVELY_ZERO_SECONDS = 0.01

interface Movement {
  running: string[]
  moving: string[]
}

async function movementOn(page: Page): Promise<Movement> {
  return page.evaluate((threshold) => {
    const longestOf = (durations: string) =>
      Math.max(...durations.split(',').map((duration) => parseFloat(duration) * (duration.trim().endsWith('ms') ? 0.001 : 1)))
    const describe = (element: Element) =>
      `${element.tagName.toLowerCase()}${element.getAttribute('data-slot') ? `[${element.getAttribute('data-slot')}]` : ''}`

    const running = document
      .getAnimations()
      .filter((animation) => animation.playState === 'running')
      .map((animation) => {
        const target = animation.effect instanceof KeyframeEffect ? animation.effect.target : null
        return `${animation.constructor.name} on ${target ? describe(target) : 'the document'}`
      })

    const moving: string[] = []
    for (const element of Array.from(document.querySelectorAll('*'))) {
      for (const pseudo of [null, '::before', '::after']) {
        const style = getComputedStyle(element, pseudo)
        const animates = style.animationName !== 'none' && longestOf(style.animationDuration) > threshold
        const transitions = style.transitionProperty !== 'none' && longestOf(style.transitionDuration) > threshold
        if (animates) moving.push(`${describe(element)}${pseudo ?? ''} animates ${style.animationName}`)
        if (transitions) moving.push(`${describe(element)}${pseudo ?? ''} transitions ${style.transitionProperty}`)
      }
    }
    return { running, moving }
  }, EFFECTIVELY_ZERO_SECONDS)
}

const loading = NINE_SCREENS.filter((state) => state.name === 'loading')

for (const screen of SCREENS) {
  test.describe(screen.name, () => {
    test('reduced motion stops movement', async ({ browser, request }) => {
      await visitEachScreen(
        browser,
        request,
        { viewport: screen.viewport, reducedMotion: 'no-preference' },
        async (page) => {
          const movement = await movementOn(page)
          expect(movement.moving, 'without a preference the skeleton breathes').toContainEqual(
            expect.stringContaining('animates breathe'),
          )
          expect(movement.running.length).toBeGreaterThan(0)
        },
        loading,
      )

      const stillMoving: string[] = []
      await visitEachScreen(browser, request, { viewport: screen.viewport, reducedMotion: 'reduce' }, async (page, state) => {
        const skeletons = page.locator('[data-slot="skeleton"]')
        if (state.name === 'loading') await expect(skeletons.first()).toBeVisible()
        const movement = await movementOn(page)
        stillMoving.push(...movement.running.map((reading) => `${state.name}: ${reading} still runs`))
        stillMoving.push(...movement.moving.map((reading) => `${state.name}: ${reading}`))
      })

      expect(stillMoving, 'movement left under reduced motion').toEqual([])
    })
  })
}
