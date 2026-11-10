# Design coverage · Soft Family R6

This record maps each of the nine design screens to a route, a role, and the states that the product covers.
The source is the OpenDesign project `currency-converter-666f/soft/`.
The e2e spec `web/e2e/design-evidence.spec.ts` captures each screen at 390, 768, and 1440 px.

## Screens

| Design screen | Route | Role | States covered | E2E specs |
|---|---|---|---|---|
| `index.html` Currency Converter | `/` | visitor, holder | From and To from the worksheet, PLN always present | `a-visitor-keeps-a-first-rate-and-converts-directly` |
| `loading.html` Finding your rate | `/result` while the request is in flight | holder | Skeleton shaped like the receipt, real pending request | `the-result-shows-its-skeleton-while-the-request-is-in-flight` |
| `result.html` Conversion result | `/result?amount&from&to&at` | holder | Direct path with one leg, cross path with two legs through PLN | `a-cross-conversion-shows-two-steps-and-their-dated-rates` |
| `missing-rate.html` A rate is missing | `/result` on `rate_missing` | holder | Names the missing direction, links to a prefilled add form | `a-missing-direction-offers-to-add-exactly-that-rate` |
| `error.html` Check your amount | `/` with field errors | visitor, holder | Client check and server field code beside the field | `an-invalid-amount-is-explained-beside-the-field` |
| `rates.html` Your rates | `/rates` | holder | List of dated rates, remove per row | `a-removed-rate-leaves-the-list` |
| `empty.html` No rates yet | `/rates` and `/result` without a key or rates | visitor | How to begin, no request without a key | `rates-without-any-rate-show-how-to-begin` |
| `rate-add.html` Add a dated rate | `/rates/new` | visitor, holder | First keep starts a worksheet, refusals beside the field | `a-visitor-keeps-a-first-rate-and-converts-directly` |
| `method.html` How conversion works | `/how-it-works` | anyone | Static rules from ADR 0003 | `every-route-has-no-axe-violations` |

## Checks on every screen

| Check | Spec |
|---|---|
| No sideways scroll at 390, 768, and 1440 px | `every-route-fits-the-screen-without-sideways-scroll` |
| Tap targets of at least 44 px | `a-control-answers-to-a-thumb` |
| Keyboard path and visible focus | `every-action-is-reachable-by-keyboard-with-visible-focus` |
| Reduced motion stops movement | `reduced-motion-stops-movement` |
| Zero axe violations | `every-route-has-no-axe-violations` |

## Not applicable

The R6.1 upload refinement does not apply. This product has no file workflow, so no upload control ships.

## Screenshots

The screenshots are in the factory evidence directory for this item.
The path is `~/.local/share/orchestrator/evidence/tehlivets-currency-converter-1/screenshots/`.
Each file is `<screen>-<width>.png`, with screen names `converter`, `loading`, `result`, `missing-rate`, `error`, `rates`, `empty`, `rate-add`, and `method`.
