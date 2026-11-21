import { readdirSync, readFileSync } from 'node:fs'
import { join } from 'node:path'
import { describe, expect, it } from 'vitest'

const SOURCE_ROOT = join(import.meta.dirname, '..', '..')
const THE_DOOR = join('components', 'ui', 'provider-door.tsx')

function sourceFilesUnder(directory: string): string[] {
  return readdirSync(directory, { withFileTypes: true }).flatMap((entry) => {
    const path = join(directory, entry.name)
    if (entry.isDirectory()) return sourceFilesUnder(path)
    return /\.(tsx?|css)$/.test(entry.name) && !entry.name.endsWith('.test.ts') ? [path] : []
  })
}

function filesNaming(pattern: RegExp): string[] {
  return sourceFilesUnder(SOURCE_ROOT)
    .filter((path) => pattern.test(readFileSync(path, 'utf8')))
    .map((path) => path.slice(SOURCE_ROOT.length + 1))
}

const luminance = (hex: string) => {
  const [r, g, b] = [1, 3, 5].map((at) => parseInt(hex.slice(at, at + 2), 16) / 255)
  const linear = (channel: number) => (channel <= 0.03928 ? channel / 12.92 : ((channel + 0.055) / 1.055) ** 2.4)
  return 0.2126 * linear(r) + 0.7152 * linear(g) + 0.0722 * linear(b)
}

const contrast = (one: string, other: string) => {
  const [lighter, darker] = [luminance(one), luminance(other)].sort((a, b) => b - a)
  return (lighter + 0.05) / (darker + 0.05)
}

const cssToken = (name: string) => {
  const css = readFileSync(join(SOURCE_ROOT, 'app', 'globals.css'), 'utf8')
  return (css.match(new RegExp(`--color-${name}:\\s*(#[0-9a-fA-F]{6})`))?.[1] ?? '').toUpperCase()
}

describe('a provider keeps its own colours and its own mark', () => {
  it("a provider's colours paint only its own door", () => {
    expect(filesNaming(/(?:bg|text|border|font|ring|fill|stroke)-provider-door/).filter((file) => file !== THE_DOOR && !file.endsWith('globals.css'))).toEqual([])
  })

  it("a provider's mark is drawn only by its own door", () => {
    expect(filesNaming(/\/brand\//)).toEqual([THE_DOOR])
  })

  it('the provider door is readable', () => {
    expect(contrast(cssToken('provider-door-ink'), cssToken('provider-door'))).toBeGreaterThanOrEqual(4.5)
    expect(contrast(cssToken('provider-door-ink'), cssToken('provider-door-pressed'))).toBeGreaterThanOrEqual(4.5)
    expect(contrast(cssToken('provider-door-edge'), cssToken('surface'))).toBeGreaterThanOrEqual(3)
  })

  it('writes the Google values exactly as Google publishes them', () => {
    expect([cssToken('provider-door'), cssToken('provider-door-edge'), cssToken('provider-door-ink'), cssToken('provider-door-pressed')]).toEqual([
      '#FFFFFF',
      '#747775',
      '#1F1F1F',
      '#F2F2F2',
    ])
  })
})
