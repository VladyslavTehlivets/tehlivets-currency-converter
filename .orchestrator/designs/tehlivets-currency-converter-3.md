# tehlivets-currency-converter-3 · Design: the sign-in door

Designer role, 2026-11-20. The plan is `.orchestrator/specs/tehlivets-currency-converter-3.md`.
The reference door is `project-orchestrator-8.md` (ADR-050). This document adapts it to Soft Family R6.
`DESIGN.md` is the token reference. This item adds its `Provider door` section.

## Artefacts

| File | What it is |
|---|---|
| `tehlivets-currency-converter-3/door.html` | The mockup. Open it bare for a gallery of 11 scenes at 390px. `?scene=<id>` opens one scene at any width. |
| `tehlivets-currency-converter-3/render.mjs` | Renders every scene at 390, 1440, and 320px and prints the measurements. |
| `tehlivets-currency-converter-3/brand/` | Google G (1x, 2x, 3x) and Google Sans Medium with its licence. Copied from the reference. |
| `tehlivets-currency-converter-3/screens/` | 22 PNGs and `measurements.json`. Not committed. Uploaded as `design-render` artefacts. |
| Stitch project `tehlivets-currency-converter-3` (`14439312630491832305`) | Two prompts (390px, 1440px) with the `DESIGN.md` overview, colours, and typography. Both timed out and returned no screen. To save the monthly quota, there was no second generation. |

Measured on 33 renders (11 scenes at 390, 1440, and 320px):

- No sideways scroll. No button label is clipped.
- No target is under 44px, painted or through the `reachesAThumb` reach.
- Every input is 16px.
- The door is 44px tall in Google Sans: 294px wide at 390px, 422px at 1440px, 224px at 320px.

## Screens

One new route: `/sign-in`. It is a static page in `web/src/app/sign-in/`. No sign-up page exists, because the door admits returning and new members.

### Architecture (frontend-ui-stack, phase 1)

| Concern | Owner | Rule |
|---|---|---|
| Session | Java server | `/sign-in` with a valid session returns 303 to `next`. The page never reads the session. |
| `refused`, `door`, `next` | URL | Read with `useSearchParams`, as `converter.tsx` does. |
| Notice copy | pure function `refusalNoticeFor(code)` in `web/src/lib/` | Closed set of six codes. Any other value gives *did not finish*. Unit-test it. |
| Busy door | client state in the page | One boolean. Reset on `pageshow` with `persisted`. |
| Test door | lazy client island | Fetch `GET /auth/test/door`. Render only on 200. Load its chunk with `next/dynamic`. |
| No provider door | client check | Hide the door on `http:` hosts that are not loopback. |

The page file holds the composition only. `ProviderDoor`, `SignInNotice`, and `TestDoor` are children.

### Composition at 1440px (`idle-1440.png`)

The page reuses the shell: `SiteHeader`, `main`, `SiteFooter`. The layout mirrors the converter hero, so the product stays familiar (Jakob's law).

- Hero grid: `md:grid-cols-[1.02fr_1fr]`, `items-center`, `md:min-h-142.5`, `lg:gap-17.5`.
- Left column: `Eyebrow` *Your account*. `<h1>` *Sign in to Currency Converter* in `text-page-title`, `max-w-[15ch]`. Lede in `text-lead`, `max-w-[43ch]`: *Use your Google account. Worksheets still open by their own key, signed in or not.*
- Right column: the halo and a `Bezel`. Inside the core, top to bottom:
  1. The notice, only when a code arrives.
  2. The `ProviderDoor` for Google, full width.
  3. A caption in `text-caption text-on-surface-variant`, 12px below: *Google sends you back here when you finish.*
- The dev test door sits under the bezel in the same column, 24px below.
- The footer adds *Sign in* after the primary links. It is the only entry point. `aria-current="page"` marks it on this route.

### Composition at 390px (`idle-390.png`)

- One column: header, eyebrow, heading, lede, bezel, test door (dev), footer.
- The door top is at 436px of 844px. That is the lower middle, inside the natural thumb zone.
- A notice pushes the door down by its own height. The door stays above 620px in every scene.

### The provider door (`ProviderDoor`, the one new primitive)

- File: `web/src/components/ui/provider-door.tsx`. Props: `door: "google"`, `busy: boolean`. No `className`.
- Anatomy: `min-h-11`, full width, `rounded-full`, 1px `border-provider-door-edge`, `bg-provider-door`.
- Label: `font-provider-door`, 500, 16px, `text-provider-door-ink`, `whitespace-nowrap`.
- Mark: `<img alt="">`, 20px, `gap-2.5` from the label, `px-3` side padding. Mark and label centre as one group.
- Hover: `bg-provider-door-pressed`. Press: the same fill, `border-provider-door-ink`, `active:scale-97`.
- Focus: the base `:focus-visible` ring (3px `on-primary-container`, offset 4px).
- Markup: `<form method="post" action="/auth/google/start">`, a hidden `next`, then `<button type="submit">`. Never an `<a>`, because browsers prefetch links.
- With JavaScript off, the door works. Only the busy state is lost.

### What the person does

| From | Action | Result |
|---|---|---|
| idle | presses the door | busy, then Google, then `next` signed in, or back here with a code |
| a notice | presses the door | the notice clears, and the door goes busy |
| Back from Google | `pageshow` restores the page | idle, with no spinner |
| footer on any route | presses *Sign in* | `/sign-in?next=<current path>` |
| dev | submits the test door | signing in, then `next`, or refused |

### Scenes

| Scene | Shows |
|---|---|
| `idle` | One Google door and the caption |
| `redirecting` | Spinner and *Opening Google…* |
| `cancelled` | Neutral notice |
| `did-not-finish` | Trouble notice for `state-mismatch`, `token-for-another-client`, or an unknown code |
| `email-unverified` | Trouble notice |
| `not-invited` | Trouble notice |
| `provider-unavailable` | Trouble notice |
| `no-provider-door` | The http dev mirror: neutral notice, no door, the test door |
| `test-door`, `-signing-in`, `-refused` | The dev test door in each state |

## States

The page has one list (the doors) and one form (the test door).

### The door list

| State | What shows |
|---|---|
| Empty | `no-provider-door`. On an `http:` host that is not loopback, the neutral notice replaces the door: *Google sign-in needs the secure address. This copy of Currency Converter runs on a local address. Google cannot send you back to it.* Add *Open the secure address* only when a secure address is configured. Dev today has none. |
| Loading | No skeleton. Nothing waits for the network. The door's loading state is `redirecting`: `disabled`, `aria-busy="true"`, a 20px spinner in place of the mark, label *Opening Google…*. A polite live region says *Opening Google*. Feedback is immediate on submit. |
| Error | A `SignInNotice` above the door, from the return code. The door stays usable. |
| Success | No screen. The callback sets the session and returns 303 to `next`, or to `/` when `next` is not a same-origin path. |

### Notices

- The callback redirects to `/sign-in?refused=<code>&door=google`. The page reads only the six codes.
- After the notice renders, `history.replaceState` removes `refused` and `door` and keeps `next`.
- The notice takes focus on arrival (`tabIndex={-1}`, no ring). The next Tab reaches the door.
- A door press or a reload clears the notice. It has no close button.
- `SignInNotice` is a `div` with a `strong` title and a `p` body. It extends `Notice` with a `neutral` tone.

| Code | Tone | Role | Title | Body |
|---|---|---|---|---|
| `cancelled` | neutral | `status` | You cancelled at Google | Nothing was shared with Currency Converter. Continue whenever you are ready. |
| `state-mismatch`, `token-for-another-client`, unknown | trouble | `alert` | That sign-in did not finish | A sign-in must finish in the browser that started it, within ten minutes. Start again below. |
| `email-unverified` | trouble | `alert` | Google has not verified this account's email | Continue with a Google account whose email is verified. |
| `not-invited` | trouble | `alert` | This Google account cannot sign in here | Continue with another Google account. |
| `provider-unavailable` | trouble | `alert` | Google is not answering | Try again in a minute. |

After `email-unverified` or `not-invited`, the next start adds `prompt=select_account`.
A notice never shows an email address or free text from the URL.

### The test door (form, dev only)

| State | What shows |
|---|---|
| Absent | Production. `GET /auth/test/door` returns 404. The page renders nothing and loads no test door code. |
| Empty | Both fields empty and labelled. The button is enabled. |
| Error, empty field | On submit only. *Enter the test member email.* or *Enter the test member password.* beside the field, with `aria-invalid` and `aria-describedby`. Focus moves to the first refused field (`focus-the-first-refusal.ts`). |
| Loading | Fields `readOnly`. The button is busy: spinner, *Signing in…*, `aria-busy`. |
| Error, refused | Both fields `aria-invalid`. Below them, `role="alert"`: *That email and password do not match the test member.* It never says which field was wrong. |
| Success | No screen. 303 to `next`. |

The test door region appears below the bezel after the probe answers. Nothing above it moves.

Region anatomy: 1px dashed `border-on-surface-variant`, `bg-surface-container`, `rounded-core`, `p-card`.
It opens with a *Dev only* badge and the line *The test member's door. Production never serves it.*
Labels: *Test member email* (`autocomplete="username"`) and *Test member password* (`autocomplete="current-password"`). The factory finds the fields by these labels.
Button: `Button variant="muted" size="commit"`, full width, *Sign in as the test member*.

### Motion

| Motion | Job | Reduced motion |
|---|---|---|
| Door goes busy | Confirm the press inside 400ms | The spinner turns once. The label still says *Opening Google…*. |
| Notice arrives | Show the new message | The page `animate-enter` rule stops. The notice appears in place. |

## Tokens

New tokens, added to `DESIGN.md` first with their reason (section *Provider door*). Put them in `@theme` in `globals.css`:

| Token | Value | Only in |
|---|---|---|
| `--color-provider-door` | `#FFFFFF` | `provider-door.tsx` |
| `--color-provider-door-edge` | `#747775` | `provider-door.tsx` |
| `--color-provider-door-ink` | `#1F1F1F` | `provider-door.tsx` |
| `--color-provider-door-pressed` | `#F2F2F2` | `provider-door.tsx` |
| `--font-provider-door` | `"Google Sans", var(--font-body)` | `provider-door.tsx` |

Existing tokens and primitives, reused by name:

| Role | Token or primitive |
|---|---|
| Page, text, secondary text | `surface`, `on-surface`, `on-surface-variant` |
| Heading | `text-page-title`, `Eyebrow` |
| Work surface | `Bezel` (`outerBezel`, `innerCore`), `--gradient-halo` |
| Neutral notice | `bg-field`, title and body `text-on-surface` |
| Trouble notice | `bg-error-container`, title `text-error`, body `text-on-surface` |
| Notice shape | `rounded-note`, `px-4.5 py-3.75`, `text-caption`, title `text-label-lg font-semibold` |
| Field error | `text-error`, `aria-invalid:border-error` (`fieldSurface`) |
| Test door region | `border-on-surface-variant` dashed, `bg-surface-container`, `rounded-core`, `p-card` |
| Dev badge | `bg-placeholder`, `text-on-surface`, `text-eyebrow` uppercase, dot in `on-surface-variant` |
| Controls | `Button` (`muted`, `commit`), `Input`, `Label`, `FormField`, `reachesAThumb` |
| Focus | base `:focus-visible` ring |
| Motion | `ease-spring`, `--duration-control`, `animate-enter` |

Measured contrast: `error` on `error-container` 5.6:1. `on-surface` on `field` 13.3:1. `on-surface-variant` on `surface` 5.8:1. `on-surface` on `placeholder` 12.4:1.

Guards to add beside `colour-comes-from-a-token.test.ts`:

- `a provider colour paints only its own door`: a `provider-door` utility appears only in `provider-door.tsx`.
- `the Google mark is drawn only by its own door`: `/brand/` appears only in `provider-door.tsx`.
- `the provider door is readable`: ink on door and on pressed at least 4.5:1, edge on `surface` at least 3:1.

## Mobile

- Thumb reach: the door is at 436px of 844px at 390px. A notice moves it down, never out of the lower half.
- Targets: the door, inputs, and the test door button are 44px tall (`min-h-11`). Footer links reach 44px through `reachesAThumb`.
- Safe areas: the shell does not set `viewport-fit=cover`, so the browser keeps content out of the notch and the home bar. Keep it so. If a later item sets it, the shell padding must become `max(env(safe-area-inset-*), gutter)`, as `door.html` shows.
- Viewport: `min-h-dvh` on `body` (already in `globals.css`), never `100vh`. No control is fixed to the bottom edge.
- Inputs are 16px (`fieldSurface` is `text-base`), so iOS does not zoom.
- No sideways scroll at 320, 390, and 1440px. The door label never wraps. The heading wraps with `overflow-wrap: anywhere`.
- Mark asset: remove the flat white backing of the Google G to transparency, or the hover fill shows a white square.
- Screen readers: reading order is heading, notice, door, caption, test door, footer. The mark is silent, so the door is read once.

## Not doing

Out of scope, as the plan rules:

- No GitHub door and no kept door. The audience is `everyone`.
- No password page in production. No account settings, linked accounts, or sign-out screen.
- No Google One Tap, FedCM, or Google's rendered button iframe.
- No worksheet ownership. Worksheets stay keyed.
- No dark theme. The product has none.

Designer's calls under the standing directives (also in `DECISIONS.md`):

- No signed-in indicator in the header. The session cookie is HttpOnly, and a session read route is not in the plan. The item that gives members something to own adds it.
- The entry point is the footer link only. Nothing needs an account yet, so a header link would add a choice with no benefit (Hick's law).
- One message for `state-mismatch`, `token-for-another-client`, and unknown codes. The person can do one thing about all three.

Rejected findings from the design loop (`frontend-design-impeccable-design-loop`, `ux-laws-overview`, `a11y-overview`):

| Finding | Decision | Why |
|---|---|---|
| Put the door on the bottom safe-area inset at 390px, as the reference does | Rejected | Every route keeps the shell header and footer. The door at 436px is already in the thumb zone. |
| No panel around the door, as the reference does | Rejected | Every Soft R6 work surface sits in a bezel. The reference rejected it for Google's dark fill. This product is light only. |
| The long eyebrow wraps to two lines at 390px | Accepted | Changed to *Your account*. |
| The caption *You come back to the page you came from* is false without `next` | Accepted | Changed to *Google sends you back here when you finish.* |
| Test door fields vanish on the canvas | Accepted | The region takes `bg-surface-container`. |
| Empty space under the notice in `no-provider-door` | Accepted | The last notice in the core has no bottom margin. |
| Left column sits mid-height beside a tall test door at 1440px | Rejected | Dev only, used by machines. Production keeps the balanced hero. |
| Autofocus the door | Rejected | A screen reader would skip the heading. One Tab reaches the door. |
| A close button on notices | Rejected | Pressing the door or a reload clears it (Hick's law). |
