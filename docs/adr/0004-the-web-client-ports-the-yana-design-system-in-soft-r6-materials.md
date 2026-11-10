# 0004 · The web client ports the yana design system in Soft R6 materials

Status: accepted · 2026-11-09 · item tehlivets-currency-converter-1 · author: architect role

## Context

Vlad approved the Soft Family R6 design with the R6.1 upload refinement for this product.
The source is OpenDesign project `currency-converter-666f`, directory `soft/`.
The prototype ships `soft.css` and `soft.js`. `soft.js` fakes the flows with local sample state.
The reuse registry names yana-wishlist as the strongest frontend. project-orchestrator and quiz-show ported its design system.

## Options

| Option | Buys | Costs |
|---|---|---|
| (a) Port the yana stack and guards. Express the Soft R6 materials as tokens and surface declarations. | Proven primitives, scale, and browser guards. | Port work before the first view. |
| (b) Ship `soft.css` with hand-written React markup. | Fast visual match. | No guards. Raw colours and sizes drift. |
| (c) A fresh Vite and React setup. | Small bundle. | Rebuilds what yana already proved. |

## Decision

Choose (a).
- Stack: Next 16 static export, React 19, Tailwind v4 (`@theme`), `shadcn@4` CLI, `radix-ui`. The export lives in `web/out` and the API container serves it.
- Port from yana: `control-scale.ts`, `field-surface.ts`, `thumb-reach.ts`, `control-row.ts`, `native-select.tsx`, `skeleton.tsx`, and their source and browser guards.
- Add three Soft declarations: `bezel-surface.ts` (outer silver bezel 39px, inner core 32px, mobile 33px and 27px), `nested-cta.tsx` (pill with a trailing circular glyph), and the colour mesh canvas in the shell.
- Tokens come from `soft/design.tokens.json` and `soft/theme.css`. Colours stay as the product palette in `DESIGN.md`.
- Fonts: self-host `bricolage.woff2` and `instrument.woff2` with their OFL files. No other font work.
- Motion: curve `cubic-bezier(0.22, 1, 0.36, 1)`, entry 850 ms, control 450 ms. `prefers-reduced-motion: reduce` removes movement and transitions.
- Do not ship `soft.js` or `soft.css`. Do not ship sample rates.

## Deciding factor

The guards turn the design rules into tests. A copied stylesheet keeps the look but loses the checks.

## Charter drivers

- Serves: Pragmatism (reuse a proven system), Clients (screens stay decoupled from the API contract).
- Trades away: first-view speed for port work.
- Scale target: a static export on a CDN-friendly path. No server render per request.

## Consequences

- The R6.1 upload refinement does not apply. This product has no file workflow. No upload control ships.
- Rejected yana parts stay rejected for the same reasons: `Card`, Radix `Select`, `Tabs`.
