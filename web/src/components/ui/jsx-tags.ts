const namedClassString = /const ([A-Za-z_]\w*)\s*(?::\s*string\s*)?=\s*\n?\s*'([^']*)'/g;

function classNameRegionOf(tag: string): string {
  const written = tag.indexOf('className=');
  if (written < 0) return '';

  let at = written + 'className='.length;
  if (tag[at] === '"') return tag.slice(at + 1, tag.indexOf('"', at + 1));

  const opens = at;
  let depth = 0;
  while (at < tag.length) {
    if (tag[at] === '{') depth += 1;
    else if (tag[at] === '}' && --depth === 0) break;
    at += 1;
  }
  return tag.slice(opens, at + 1);
}

export function classesReaching(
  tag: string,
  source: string,
  alsoNaming: Record<string, string> = {},
): string[] {
  const written = classNameRegionOf(tag);
  if (!written) return [];

  const spelledOut = written.startsWith('{')
    ? [...written.matchAll(/'([^']*)'|"([^"]*)"|`([^`]*)`/g)].map(
        (quoted) => quoted[1] ?? quoted[2] ?? quoted[3] ?? '',
      )
    : [written];
  const composed = [...source.matchAll(namedClassString)]
    .filter(([, name]) => new RegExp(`\\b${name}\\b`).test(written))
    .map(([, , classes]) => classes);
  const imported = Object.entries(alsoNaming)
    .filter(([name]) => new RegExp(`\\b${name.replace('.', '\\.')}\\b`).test(written))
    .map(([, classes]) => classes);

  return [...spelledOut, ...composed, ...imported]
    .flatMap((classes) => classes.split(/\s+/))
    .filter(Boolean)
    .map((utility) => utility.replace(/^!/, '').split(':').at(-1) ?? '');
}

export interface OpeningTag {
  tag: string;
  at: number;
}

export function openingTagsOf(source: string, tag: string): OpeningTag[] {
  const tags: OpeningTag[] = [];
  const opening = `<${tag}`;
  let from = 0;

  while (true) {
    const start = source.indexOf(opening, from);
    if (start < 0) return tags;

    let at = start + opening.length;
    if (/[\w-]/.test(source[at] ?? '')) {
      from = at;
      continue;
    }

    let insideExpression = 0;
    while (at < source.length) {
      const character = source[at];
      if (character === '{') insideExpression += 1;
      else if (character === '}') insideExpression -= 1;
      else if (character === '>' && insideExpression === 0) break;
      at += 1;
    }

    tags.push({ tag: source.slice(start, at + 1), at: start });
    from = at + 1;
  }
}

export interface Element {
  name: string;
  tag: string;
  at: number;
  children: Element[];
}

export class TheWalkDidNotBalance extends Error {}

function looksLikeAnOpeningTag(source: string, at: number) {
  return !/[\w$)\]]/.test(source[at - 1] ?? ' ');
}

function tagEndingFrom(source: string, from: number) {
  let braces = 0;
  let quote: string | null = null;
  let at = from;

  while (at < source.length) {
    const character = source[at];
    if (quote) {
      if (character === quote) quote = null;
    } else if (character === '"' || character === "'" || character === '`') quote = character;
    else if (character === '{') braces += 1;
    else if (character === '}') braces -= 1;
    else if (character === '>' && braces === 0) break;
    at += 1;
  }

  return at;
}

export function elementTreeOf(source: string, saidToBe: string): Element[] {
  const roots: Element[] = [];
  const open: Element[] = [];

  for (let at = 0; at < source.length; at += 1) {
    if (source[at] !== '<') continue;
    const closing = source[at + 1] === '/';
    if (!closing && !looksLikeAnOpeningTag(source, at)) continue;

    let cursor = at + (closing ? 2 : 1);
    const from = cursor;
    while (/[\w.]/.test(source[cursor] ?? '')) cursor += 1;
    const name = source.slice(from, cursor);
    if (!name && source[cursor] !== '>') continue;

    const ending = tagEndingFrom(source, cursor);
    const startsAt = at;
    const tag = source.slice(startsAt, ending + 1);
    at = ending;

    if (closing) {
      const opened = open.pop();
      if (!opened || opened.name !== (name || '<>')) {
        throw new TheWalkDidNotBalance(
          `${saidToBe}: </${name}> closes <${opened?.name ?? 'nothing'}>`,
        );
      }
      continue;
    }

    const element: Element = { name: name || '<>', tag, at: startsAt, children: [] };
    (open.at(-1)?.children ?? roots).push(element);
    if (!/\/\s*>$/.test(tag)) open.push(element);
  }

  if (open.length > 0) {
    throw new TheWalkDidNotBalance(`${saidToBe}: <${open.map((one) => one.name).join('>, <')}> left open`);
  }

  return roots;
}

export function everyElementIn(roots: Element[]): Element[] {
  return roots.flatMap((element) => [element, ...everyElementIn(element.children)]);
}
