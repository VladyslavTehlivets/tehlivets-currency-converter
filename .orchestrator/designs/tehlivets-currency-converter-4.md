# tehlivets-currency-converter-4 · design: the sign-in door

Designer role, 2026-11-20. The implementer builds this document. The reviewer checks the build against it and against the renders.

Sources:
- Plan: `.orchestrator/specs/tehlivets-currency-converter-4.md`.
- Factory reference door: `~/projects/project-orchestrator/.orchestrator/designs/project-orchestrator-8.md`. This document maps it to Soft Family R6.
- Design reference: `DESIGN.md` at the repository root. This item creates it from the approved R6 file and adds the provider door group.
- Declaration: `.orchestrator/sign-in.json` is `everyone`, `["google"]`, no kept door. Dev is `http://10.13.13.4:7070`.

Renders (not in git, uploaded as `design-render` artefacts):

| File | What it is |
|---|---|
| `tehlivets-currency-converter-4/door.html` | The mockup. `?scene=<id>` opens one scene at any width. The door is clickable. |
| `tehlivets-currency-converter-4/screens/<scene>-<390,1440>.png` | 22 renders, 11 scenes at 390x844 and 1440x900. |
| `tehlivets-currency-converter-4/render.mjs` | Renders the PNGs and measures 320, 390, and 1440 px. |
| `tehlivets-currency-converter-4/brand/` | Google G (light, 1x, 2x, 3x) and Google Sans Medium with its OFL. |
| `tehlivets-currency-converter-4/stitch/` | Stitch exploration, when Stitch returns it. Not the build reference. |

## Screens

One new screen: `/sign-in`. One shell change: the primary links get *Sign in*.

### Architecture (frontend-ui-stack, step 1)

| Concern | Decision |
|---|---|
| Route | `web/src/app/sign-in/page.tsx` in the static export. The API serves it at `GET /sign-in`. A valid session makes the API answer 302 to `next`. |
| Server state | None on the page. Nothing waits for the network before the page draws. |
| Client state | Three values: the notice from the query, door busy, test door state. |
| Refusal parser | Pure `noticeForRefusal(query)` in `web/src/lib/sign-in-refusal.ts`. It reads only the six closed codes. Any other value gives *did not finish*. Unit test every code and one unknown code. |
| Next path | Pure `safeNextPath(raw)` in the same module. One leading slash only. `//` and `/\` give `/`. The server checks it again. |
| Door list | Google door when `window.isSecureContext` is true. Otherwise the empty state. Read it after mount. |
| Test door | Ships only in a build with `NEXT_PUBLIC_E2E_TEST_IDENTITY=on`. A production build has no test door component, so nothing is hidden with CSS. It calls the API through `web/src/lib/worksheet-api.ts`. |
| Shell | `PRIMARY_LINKS` gets `{ href: SIGN_IN_ROUTE, label: 'Sign in' }`. Header nav, mobile menu, and footer show it. |

### The page, top to bottom

1. Shell header, unchanged. *Sign in* is the last primary link, `aria-current="page"` here.
2. Copy column: `Eyebrow` *Your account*. `h1` *Sign in to Currency Converter.* (`text-page-title`, `max-w-[15ch]`). Lede: *Continue with your Google account. Worksheets you open by their key stay open without it.* (`text-lead`, `max-w-[40ch]`).
3. Door column with the existing halo (`--gradient-halo`), as on the converter hero.
   - a. `Bezel` with the door stack.
   - b. Notice, only after a refusal or in the empty state. First in the bezel.
   - c. The Google door, full width.
   - d. Caption, `text-caption`: *Google sends Currency Converter your verified email. Your Google password stays with Google.*
   - e. Test door, dev build only. A separate dashed region 22px under the bezel. It is never inside the bezel.
4. Shell footer, unchanged except the new link.

### The provider door (the one new primitive)

- File: `web/src/components/ui/provider-door.tsx`. Props: `door: 'google'`, `busy: boolean`. No `className` prop for colour or size.
- A `<form method="get" action="/auth/google/start">` with a hidden `next`, then `<button type="submit">`. Never an `<a>`, because browsers prefetch links.
- 44px tall (`aThumbTall`), full width, `rounded-full` like every product control, `px-4`.
- 1px `provider-door-edge` border, `provider-door` fill, `provider-door-ink` label.
- Label *Continue with Google* in `font-provider-door` 16px. Mark `<img alt="">` 20px, 10px gap, centred with the label as one group.
- Mark: `/brand/google-g-light.png` with `srcset` 2x and 3x. Remove the white backing to transparency before use, so the pressed fill shows around the G. Do not change any G pixel.
- Hover and press: `provider-door-pressed` fill. Press also turns the edge `provider-door-ink`.
- Focus: the product ring (3px `on-primary-container`, offset 4px).

### At 390px (`idle-390.png`)

- One column, 18px gutters. The door is 294px wide inside the bezel.
- The door top is at 419px of 844px. That is the natural thumb zone. A notice pushes the door down to 511 to 552px, still in reach.
- The menu toggle stays. *Sign in* is in the mobile menu.

### At 1440px (`idle-1440.png`)

- The converter hero grid: `md:grid-cols-[1.02fr_1fr]`, `gap-17.5`, `items-center`, `min-h-142.5`.
- Copy left, door bezel right. The door is 422px wide.
- One action on the page (Hick). The door is the largest target in the right column (Fitts).

### What the person does

| From | Action | Result |
|---|---|---|
| idle | presses the door | busy, then Google, then `next` signed in, or back here with a code |
| a notice | presses the door | the notice goes and the door goes busy |
| busy | presses Back at Google | idle again (`pageshow` with `persisted` clears busy) |
| dev, no door | submits the test door | signing in, then `next`, or refused |

### Scenes

| Scene | Shows |
|---|---|
| `idle` | The Google door and its caption |
| `redirecting` | Door busy: spinner and *Opening Google…* |
| `cancelled`, `did-not-finish`, `email-unverified`, `not-invited`, `provider-unavailable` | The notice for each code over the door |
| `test-door` | Door and test door, a dev build on a secure origin such as localhost |
| `no-provider-door` | Dev on its http address: the empty state and the test door |
| `test-door-signing-in`, `test-door-refused` | The test door states |

## States

The page has one list (the doors) and one form (the test door).

### The door list

| State | What shows |
|---|---|
| Empty | No secure origin, as on dev `http://10.13.13.4:7070`. A `quiet` notice replaces the door and caption: *Google sign-in needs a secure address* / *This copy of Currency Converter runs on a local http address, where Google cannot send you back.* No link, because no https environment exists yet. On a dev build the test door follows. |
| Loading | No skeleton, because nothing loads. The busy door is the loading state: `aria-busy="true"`, `disabled`, full opacity, a 20px spinner in place of the mark (`animate-spin`, ring `border-provider-door-ink/25`, arc `border-t-provider-door-ink`), label *Opening Google…*. A polite live region says *Opening Google*. Enter busy from the form submit event, so disabling the button cannot cancel the submit. |
| Error | A notice above the door, from the return code. The door stays usable. |
| Success | No screen. The callback sets the session and redirects to `next`. |

### Notices from `?refused=<code>&door=google`

- Draw the notice, then remove the query with `history.replaceState`. A reload shows idle.
- Focus the notice on arrival (`tabindex="-1"`, no ring). The next Tab reaches the door.
- The notice has no close button. Pressing the door clears it.
- The notice never shows an email address or free text from the URL.
- `SignInNotice` in `web/src/components/sign-in-notice.tsx`: a `div` with the `Notice` shape (`rounded-note px-4.5 py-3.75 text-caption`). Title `text-label-lg font-semibold`, body below. No icons: the product has no icon set, and a new dependency is not worth one page.

| Code | Tone | Role | Title | Body |
|---|---|---|---|---|
| `cancelled` | quiet | `status` | You cancelled at Google | Nothing was shared with Currency Converter. Continue whenever you are ready. |
| `state-mismatch`, `token-for-another-client`, unknown | quiet | `alert` | That sign-in did not finish | A sign-in must finish in the browser that started it, within ten minutes. Start again below. |
| `email-unverified` | trouble | `alert` | Google has not verified this account's email | Continue with a Google account whose email is verified. |
| `not-invited` | trouble | `alert` | This Google account cannot sign in here | Continue with another Google account. |
| `provider-unavailable` | quiet | `alert` | Google is not answering | Try again in a minute. |

- Tone `quiet` means nothing is wrong with the account. Start again. Tone `trouble` means this account cannot get in as it is.
- After `email-unverified` or `not-invited`, the next start adds `prompt=select_account`.
- `not-invited` is not expected, because the audience is everyone. The code set is closed, so the copy exists.

### The test door (dev build only)

| State | What shows |
|---|---|
| Empty (`test-door`, `no-provider-door`) | *Dev only* badge, the line *The test member's door. Production never serves it.*, two labelled empty fields, the button enabled. |
| Loading (`test-door-signing-in`) | Fields `readonly`. Button `aria-busy`, `disabled`, label *Signing in…*. |
| Error (`test-door-refused`) | Both fields `aria-invalid` with `aria-describedby="test-door-error"`. `FormError` with `role="alert"`: *That email and password do not match the test member.* It never says which field. |
| Error, no answer | `FormError`: *The sign-in service did not answer. Try again.* Fields keep their values. |
| Success | No screen. Redirect to `next`. |

- Labels are exact, because factory runs find the fields by them: *Test member email* (`type=email`, `autocomplete=username`) and *Test member password* (`autocomplete=current-password`).
- Button: `Button variant="secondary" size="commit"`, full width, *Sign in as the test member*. Never the `primary` nested CTA, so it never looks like the real door.

### Motion

- Door busy: label change and spinner. Reduced motion stops the spinner through the global rule. The label still tells the state.
- The bezel enters with `animate-enter`, as on every page. Reduced motion removes it.

## Tokens

All from `DESIGN.md` and `web/src/app/globals.css`. One new group, the provider door group, added to `DESIGN.md` with its reason.

| Role | Token |
|---|---|
| Canvas, mesh, halo | `surface`, `--gradient-mesh`, `--gradient-halo` |
| Heading, body text | `on-surface`, `on-surface-variant`, `text-page-title`, `text-lead`, `text-caption` |
| Work surface | `Bezel` (`outerBezel`, `innerCore`, `rounded-bezel`, `rounded-core`, `p-card`) |
| Quiet notice | `bg-field` + `text-on-surface` (13.3:1) |
| Trouble notice | `bg-error-container` + `text-error` title (5.6:1) + `text-on-surface` body (12.3:1) |
| Test door region | `bg-surface-container`, 1px dashed `border-on-surface-variant`, `rounded-core`, `p-card` |
| Dev badge | `bg-placeholder` + `text-on-surface` (12.4:1), eyebrow type |
| Field error | `aria-invalid:border-error`, `text-error` |
| Focus | `on-primary-container` ring from the base layer |
| Provider door (door only) | `provider-door`, `provider-door-edge`, `provider-door-ink`, `provider-door-pressed`, `font-provider-door` |

Add to `globals.css` `@theme`: `--color-provider-door: #FFFFFF`, `--color-provider-door-edge: #747775`, `--color-provider-door-ink: #1F1F1F`, `--color-provider-door-pressed: #F2F2F2`, `--font-provider-door: "Google Sans", var(--font-body)`. Add an `@font-face` for `/fonts/google-sans-medium-latin.woff2` with `font-display: swap`. Preload it on `/sign-in` only.

Primitives reused by name: `Bezel`, `Eyebrow`, `Notice` shape, `Button` (`secondary`, `commit`), `Input`, `Label`, `FormField`, `FormError`, `reachesAThumb`, `aThumbTall`, `entersOnArrival`. `PageHead` is not used, because the hero grid replaces it.

Guards to add (classical, they read the sources):
- `a provider's colours paint only its own door`: `provider-door*` utilities appear only in `provider-door.tsx`.
- `a provider's mark is drawn only by its own door`: `/brand/` is referenced only from `provider-door.tsx`.
- `the provider door is readable`: ink on fill and on pressed at least 4.5:1, edge on `surface` at least 3:1.
- The existing hex guard scans `.ts` and `.tsx` only. The provider hex values live in `globals.css`, so no exemption is needed.

## Mobile

- Thumb reach: the door top sits at 419px of 844px at 390px. A notice grows the stack down, to at most 552px. Both are in the natural thumb zone. The test door fields sit lower. That is fine, because machines use them.
- Targets: door, inputs, and test door button are 44px tall. Nav and footer links use `reachesAThumb`. Measured: no target under 44px in any scene at 320, 390, or 1440px.
- Safe areas: the shell keeps the default `viewport-fit=auto`, so the browser keeps the page out of the notch and the home bar. Do not add `viewport-fit=cover` for this page alone. The shell bottom padding (`pb-9`) keeps the footer links above the home bar.
- Viewport: `min-h-dvh` from the base layer. Never `100vh`.
- Inputs are 16px (`fieldSurface` is `text-base`), so iOS does not zoom.
- No sideways scroll: measured 0px at 320, 390, and 1440px in all 11 scenes. The heading and notices use `overflow-wrap: anywhere`. The door label never wraps. It fits a 224px door at 320px.
- Screen reader order: heading, lede, notice (focused on arrival), door, caption, test door, footer. The G image is silent, so the door is read once by its label.

## Not doing

- No GitHub door and no kept door. The declaration is `everyone`, `["google"]`.
- No signed-in indicator, account menu, or sign-out in the shell. The static export cannot read the session without a route the plan does not name. A later item can add `GET /v1/session` and the menu.
- No dark theme. The product has a light theme only.
- No icons in notices. The product has no icon set.
- No link to an https environment in the empty state. None exists yet. Add it when `orchestrator env list` shows one.
- No Google One Tap, FedCM, or Google button iframe.
- No sign-up page and no *remember me*.
- No localisation beyond English.

### Findings from the impeccable loop that I rejected, and why

Lenses: `frontend-design-impeccable-design-loop`, `ux-laws-overview` (Fitts, Hick, proximity, Von Restorff), `a11y-overview`.

- **Put the doors on the bottom safe-area inset at 390px, as the reference does.** Rejected. The product keeps its shell header and footer on every page. The door at 419px of 844px is already in the thumb zone.
- **Drop the bezel, as the reference does at 1440px.** Rejected. The reference dropped the panel for its dark theme. This product is light only, and `DESIGN.md` puts every work surface in a bezel.
- **Make the door the green nested CTA.** Rejected. Google fixes its button colours. A green door would break the brand rules.
- **Add an amber attention tone for *did not finish* and *not answering*.** Rejected. The palette has no amber. Two tones are enough: start again (quiet) or this account cannot get in (trouble).
- **Show the test door hint *Use the test member's door below* in the empty notice.** Removed after the first render. A dev build without the flag would show a hint to a door that is not there.
- **Leave 18px under a lone notice.** Fixed after the first render. The last notice in the bezel has no bottom margin.
- **A close button on notices.** Rejected (Hick). The door and a reload both clear it.
- **Autofocus the door.** Rejected. A screen reader would skip the heading.
