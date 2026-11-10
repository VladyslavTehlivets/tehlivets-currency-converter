import { readdirSync, readFileSync } from 'node:fs';
import { join } from 'node:path';
import { describe, expect, it } from 'vitest';
import { openingTagsOf } from './jsx-tags';

const SOURCE_ROOT = join(import.meta.dirname, '..', '..');
const WHERE_A_CONTROL_IS_MADE = join('components', 'ui');

function screenFilesUnder(directory: string): string[] {
  return readdirSync(directory, { withFileTypes: true }).flatMap((entry) => {
    const path = join(directory, entry.name);
    if (entry.isDirectory()) return path.includes(WHERE_A_CONTROL_IS_MADE) ? [] : screenFilesUnder(path);
    return entry.name.endsWith('.tsx') ? [path] : [];
  });
}

function handRolledControls(): { found: string[]; filesRead: number } {
  const found: string[] = [];
  let filesRead = 0;

  for (const path of screenFilesUnder(SOURCE_ROOT)) {
    filesRead += 1;
    const source = readFileSync(path, 'utf8');
    for (const { tag } of openingTagsOf(source, 'button')) {
      found.push(`${path.slice(SOURCE_ROOT.length + 1)}: ${tag.split('\n')[0]}`);
    }
  }

  return { found, filesRead };
}

describe('a control is a primitive, so what it answers to is decided once', () => {
  it('leaves no screen to hand-roll a button that reaches for itself', () => {
    expect(handRolledControls().found).toEqual([]);
  });

  it('reads the screens that actually draw controls', () => {
    expect(handRolledControls().filesRead).toBeGreaterThan(20);
  });
});
