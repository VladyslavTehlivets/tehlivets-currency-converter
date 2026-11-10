# tehlivets-currency-converter-1 design hand-off

Designer decision, 2026-11-10. This document completes the missing design hand-off for the existing item branch.
You build and review against the approved nine-view family and the state requirements below.
[DESIGN.md](../../DESIGN.md) defines the only shared token scale.
The [architecture plan](../specs/tehlivets-currency-converter-1.md) and [API contract](../../docs/api.md) define application behavior.

## Screens

The source is the existing OpenDesign project `currency-converter-666f`, under `soft/`.
The nine HTML files, stylesheet, and script match the source read through OpenDesign MCP.
Existing source projects and archives remain available. No source project is deleted or replaced.

[Human review](https://o-design.vtehsol.com/api/projects/currency-converter-666f/raw/soft/REVIEW.html) shows the approved family.
[Visual system](https://o-design.vtehsol.com/api/projects/currency-converter-666f/raw/soft/DESIGN-SYSTEM.html) shows the approved materials.
The supplied frozen archive checksum is `5bb8daefa5c0de1026a11d343d21633fd23f4127b2686d7a16fa94b86948d8f6`.
This run reads only the Currency Converter project. It does not fetch the separate personal-project archive.

Roles are visitor and worksheet holder. A visitor has no worksheet key. A holder has the browser worksheet key.
No account screen, administrator screen, or invented authorization role exists.

| Approved view | Route and role | 1440px composition and action | 390px composition and action |
| --- | --- | --- | --- |
| `index.html` | `/`, visitor or holder | Hero copy left, conversion bezel right. Submit amount, direction, and date. | Copy precedes one-column form. Keep the submit action beside the final field. |
| `result.html` | `/result?amount&from&to&at`, holder | Exact result and path left, selected rates right. Offer another conversion. | Result, path, then dated legs. Wrap exact amounts without truncation. |
| `rates.html` | `/rates`, holder | Page heading and Add a rate. Show direction, value, date, and Remove per row. | Stack labeled row values inside the bezel. Keep Remove separate from Add a rate. |
| `rate-add.html` | `/rates/new`, visitor or holder | One form bezel. Pair From and To, then rate and date. Submit Keep rate. | Stack all four fields. Keep the action directly below the date. |
| `method.html` | `/how-it-works`, anyone | Reading surface explains directional selection, nearest date, exact multiplication, and PLN routing. | Keep the same reading order in one column. Offer conversion after the explanation. |
| `missing-rate.html` | `/result`, holder, `rate_missing` | Show No result yet and the exact missing direction. Add that rate is primary. | Place the missing direction before the action. Keep Change the conversion secondary. |
| `empty.html` | `/rates` or `/result`, visitor or empty holder | Explain Start with a rate. Offer Keep your first dated rate. | One useful empty surface and one primary action. Do not show an empty grid. |
| `loading.html` | `/result`, holder, request pending | Keep the receipt silhouette and Finding the closest dated rate message. | Stack the receipt silhouette. Keep request context visible. Do not show a fabricated amount. |
| `error.html` | `/`, visitor or holder, invalid input | Keep the conversion form and submitted values. Show the error beside Amount. | Keep labels and errors above the next field. Focus Amount after failed submission. |

At 768px, use the tablet shell and the menu. Keep paired form fields when they fit.
Use one column for the receipt if its exact amounts cannot fit both columns.
The shell, navigation, footer, and control styling stay consistent on every route.
Use Convert, Rates, and How it works as the three navigation choices.
Apply Hick's law through one primary task and progressive recovery actions.
Apply Fitts's law through nearby actions, separated destructive controls, and 44px minimum targets.
Repeat the amount, direction, and requested date instead of requiring you to remember them.

Journey A: Convert → empty worksheet → keep a first rate → rates → convert → receipt.
Journey B: Convert → missing direction → prefilled rate form → rates → retry conversion.
Journey C: Invalid amount → inline correction → pending request → exact receipt.
Journey D: Rates → remove → confirmed list or empty worksheet.
Keep conversion query values during correction and recovery. Never put the worksheet key in a URL.
Prefill the missing direction and requested moment when you open the rate form.

Architecture comes before primitives and tokens.
The API owns worksheets, rates, authorization, nearest-date selection, PLN routing, and exact results.
The client owns draft strings, query parameters, menu expansion, focus, and pending presentation.
Keep `WorksheetAnswer`, `ConversionOutcome`, and `RateBook` as separate typed boundaries.
Keep `DatedRate[]` as the server list representation, capped at 500 entries.
Use the existing API client and feature components. Do not add a global store or a second conversion engine.
Reuse the existing yana-derived controls and guards identified in ADR 0004 and the reuse registry.
The existing Java Money and converter model remain the domain reference.

## States

Every request acknowledges your action within 400ms. Pending states follow real requests and never use progress timers.
The HTML mockups contain clearly separate sample data. Production starts without sample rates.

| Surface | Empty | Loading | Error | Success |
| --- | --- | --- | --- | --- |
| Conversion form | Blank Amount, labeled direction controls, current local date. Show Add a rate when currencies are unavailable. | Reserve the fields while rates load. Convert submission opens the receipt skeleton. | Preserve all values. Associate each error with its field. Focus the first invalid field. | Open the receipt only after an API success. |
| Rate form | Blank Rate, valid currency choices, current local date. Preserve recovery prefill. | Show Keeping rate and prevent duplicate submission during the real request. | Preserve draft values. Explain decimal, direction, date, duplicate, and worksheet-cap refusals. | Open Rates after confirmed storage. Announce Rate kept. |
| Rates list | Show the approved empty view and Keep your first dated rate. | Reserve three row silhouettes in the same bezel. Announce Loading your rates. | Show The worksheet did not answer. Try again. Offer Retry without hiding the heading. | Show only server-confirmed rows. Announce stored and removed rates through a polite status region. |
| Rate removal | Removing the last rate opens the approved empty view. | Keep the row visible and mark only its Remove action pending. | Keep the row and show its failure beside it. Offer Retry. | Remove the row after confirmation. Focus the next row action or the empty-state action. |
| Receipt and path | With no worksheet, show the approved empty view. | Show the approved receipt skeleton until the conversion answers. | Missing direction uses the approved missing-rate view. Network failure offers Try again and Change the conversion. | Show exact result, source amount, requested moment, each selected rate, and each selected rate date. |
| Method | Static copy has no empty state. | Static copy needs no request skeleton. | Navigation failure uses the application's normal error route. | Explain the complete rules and provide a conversion link. |
| Navigation | No missing-key warning appears during anonymous browsing. | Menu expansion needs no service request. | Escape closes the menu and restores focus. | Indicate the current route without relying on color alone. |

Name every refusal and its recovery action.

| Refusal | Placement and recovery |
| --- | --- |
| `invalid_amount`, `invalid_rate` | Beside the decimal field. Require a positive plain decimal with at most 15 integer digits and 12 fractional digits. |
| `invalid_currency`, `same_currency` | Beside the affected currency control. Choose valid, different currencies. |
| `invalid_moment` | Beside the date field. Choose a date from 1900 through 2100. Send the local offset. |
| `rate_exists` | Beside Rate date. Choose another moment. Keep the submitted direction and value. |
| `worksheet_full` | Form notice. Explain the 500-rate limit and link to Rates to remove one. |
| `rate_not_found` | Row notice. Explain that the rate no longer exists and reload the server list. |
| `rate_missing` | Missing-rate view. Name the missing direction and offer the prefilled rate form. |
| `401` | Clear the stale key. Explain that this browser cannot open that worksheet. Offer a new worksheet explicitly. |
| Network, `429`, or server failure | Preserve the draft. Explain that the request fails and offer a deliberate retry. Never claim storage succeeds. |

The server keeps the 4KB body limit, positive decimal checks, ISO currency checks, and worksheet authorization.
The Java library selects the nearest directional date in either time direction. The earlier date wins equal-distance ties.
PLN conversions use one leg. Other currency pairs use two legs through PLN.
Never infer a reverse rate. Never invent a missing rate. Never round an exact result.
On results and rates, show Example rates. This result is not a market quote.
Show selected rate timestamps separately from the requested conversion moment.

Errors appear after submission or a completed field interaction. Do not interrupt incomplete typing.
Use `role="alert"` for blocking refusals and `aria-describedby` for field messages.
Use `role="status"` for pending and success feedback. Hide decorative skeleton shapes from assistive technology.
Keep visible focus through Tab, Shift-Tab, Enter, Space, Escape, and native select arrow navigation.

The render bundle includes these named alternate mockups at 390px and 1440px.
They are states of the routes above, rather than additional application routes.

| Render file | Existing surface and state |
| --- | --- |
| `converter-empty.html` | Conversion form with no amount |
| `converter-loading.html` | Conversion form while the rate list loads |
| `converter-error.html` | Conversion form with a request failure |
| `rate-empty.html` | Rate form with no value |
| `rate-loading.html` | Rate form during confirmed-request waiting |
| `rate-error.html` | Rate form with decimal validation failure |
| `rate-duplicate.html` | Rate form with a duplicate direction and moment |
| `rate-full.html` | Rate form at the 500-rate limit |
| `rate-network-error.html` | Rate form with preserved draft and request failure |
| `rates-loading.html` | Rates list with row skeletons |
| `rates-error.html` | Rates list with request failure and retry |
| `rates-success.html` | Rates list after confirmed storage |
| `rates-removing.html` | Rates list during removal |
| `rates-remove-error.html` | Rates list after failed removal |
| `rates-removed.html` | Rates list after confirmed removal |
| `result-direct.html` | Receipt with one PLN leg |
| `result-long.html` | Receipt with a long exact amount |
| `result-error.html` | Conversion request failure with retry |
| `worksheet-expired.html` | Unavailable worksheet key and explicit new-worksheet action |
| `menu-open.html` | Expanded phone navigation |

## Tokens

Use the semantic colors, typography, spacing, materials, elevation, radii, focus, targets, and motion in [DESIGN.md](../../DESIGN.md).
Reuse `Bezel`, `PageHead`, `SectionHead`, `Notice`, `Button`, `NestedCtaLink`, `Input`, `NativeSelect`, `Label`, and `Skeleton`.
Reuse `ConversionForm`, `RateForm`, `ConversionReceiptView`, `RateTable`, `HowToBegin`, and `MissingRate`.
The empty-state recipe supplies title, explanation, and action through the existing `HowToBegin` composition.
The skeleton recipe supplies content-shaped placeholders through the existing `Skeleton` primitive.
Do not install duplicate component implementations for those recipes.
New reference tokens make existing materials, minimum targets, safe areas, and focus explicit. They do not change the approved typography.

The Impeccable loop uses the existing approved world. A second picker or Stitch generation would reopen a settled design.
Review the browser renders at 390px and 1440px, with 768px as the additional tablet check.
Record accepted corrections and rejected findings in the evidence record beside this document.
Keep controls uniform. Vary composition through the hero, receipt split, rate rows, and method reading surface.

Read the [rendered judgment](tehlivets-currency-converter-1-judgment.md) for accepted corrections and rejected findings.
Read the [evidence index](tehlivets-currency-converter-1-evidence.md) for Cloudflare artifacts, hashes, and check results.

## Mobile

Keep all actions and navigation targets at least 44px in both dimensions.
Keep primary actions after the related fields within the natural thumb path.
Use at least 16px input text to prevent focus zoom.
Use `100dvh` minimum page height and safe-area insets on all device edges.
Keep one-column phone forms, readable labels, and visible error text.
Wrap long amounts and timestamps. Do not create horizontal page scrolling or clip financial values.
Stack rate cells with explicit labels on phones. Preserve semantic row relationships for assistive technology.
The mobile menu opens below the header. Escape returns focus to Menu.
The skip link appears on focus and targets the main content.
Disable movement, transitions, pulsing, and smooth scrolling when you request reduced motion.
Keep pending feedback visible when motion stops.

## Not doing

- No production code changes occur in this designer run.
- No new upload control exists because this product has no file workflow.
- No live quotes, rate provider, sample production rates, accounts, or fake authentication exist.
- No prototype timers, local conversion service, or sample storage becomes a production dependency.
- No font changes, alternate visual family, or unapproved dark theme occur.
- No Java conversion changes, deployment, merge, CI repair, or next-role activity occurs in this run.
- No new images are necessary because the approved worksheet contains no placeholder photography.
- No source project or archive is deleted.
