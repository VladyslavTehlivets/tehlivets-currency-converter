import { readdirSync, readFileSync } from 'node:fs';
import { join } from 'node:path';
import { describe, expect, it } from 'vitest';

const SOURCE_ROOT = join(import.meta.dirname, '..', '..');

const RAW_HUES = [
  'slate', 'gray', 'zinc', 'neutral', 'stone',
  'red', 'orange', 'amber', 'yellow', 'lime', 'green', 'emerald', 'teal',
  'cyan', 'sky', 'blue', 'indigo', 'violet', 'purple', 'fuchsia', 'pink', 'rose',
];

const rawPaletteClass = new RegExp(
  `\\b(?:[a-z-]+:)*(?:bg|text|border|ring|from|via|to|divide|placeholder|shadow|outline|accent|decoration|fill|stroke|caret)-(?:${RAW_HUES.join('|')})-\\d{2,3}\\b`,
  'g',
);

const hexColour = /#(?:[0-9a-fA-F]{3}|[0-9a-fA-F]{6})\b/g;

const WHERE_A_COLOUR_IS_NOT_A_CLASS = 'lib/browser-chrome.ts';

function sourceFilesUnder(directory: string): string[] {
  return readdirSync(directory, { withFileTypes: true }).flatMap((entry) => {
    const path = join(directory, entry.name);
    if (entry.isDirectory()) return sourceFilesUnder(path);
    return /\.tsx?$/.test(entry.name) && !entry.name.endsWith('.test.ts') ? [path] : [];
  });
}

function offences(pattern: RegExp): string[] {
  return sourceFilesUnder(SOURCE_ROOT).flatMap((path) =>
    (readFileSync(path, 'utf8').match(pattern) ?? []).map(
      (found) => `${path.slice(SOURCE_ROOT.length + 1)}: ${found}`,
    ),
  );
}

describe('every colour on a screen comes from a token', () => {
  it('names no raw Tailwind hue', () => {
    expect(offences(rawPaletteClass)).toEqual([]);
  });

  it('writes a hex literal only where the browser chrome demands one', () => {
    expect(offences(hexColour).filter((found) => !found.startsWith(WHERE_A_COLOUR_IS_NOT_A_CLASS)))
      .toEqual([]);
  });
});
