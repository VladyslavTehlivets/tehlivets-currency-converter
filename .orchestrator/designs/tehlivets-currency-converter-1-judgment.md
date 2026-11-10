# Rendered design judgment

Item: `tehlivets-currency-converter-1`. Role: designer. Date: 2026-11-10.

## Evidence and method

The approved source remains `currency-converter-666f/soft/` in OpenDesign.
The nine local source pages, CSS, and JavaScript match the MCP source text.
The render bundle adds design-only accessibility corrections and alternate states.
The browser loads existing self-hosted approved fonts because direct public asset downloads return HTTP 403.
The original prototype files remain unchanged in git and OpenDesign.

The Impeccable loop starts from the existing approved world.
The designer inspects rendered screenshots, records findings, refines mockups, and checks the rendered result again.
The run uses no live picker, user-input wait, subagent, or new visual-world generation.

## Accepted findings

| Finding | Design correction |
| --- | --- |
| Brand and navigation targets fall below 44px. | Enlarge their hit areas while preserving the approved appearance. |
| The receipt skips from H1 to H3. | Use H2 semantics with the approved subheading appearance. |
| The invalid-amount prototype lacks a visible inline refusal. | Show the invalid value and linked Amount error. |
| The missing-rate prototype omits the missing direction. | Name the missing direction before the recovery action. |
| Rates lack a designed removal control. | Add one quiet Remove action per row and explicit request states. |
| Phone rate rows can become dense with removal and exact values. | Stack labeled cells without horizontal page scrolling. |
| Alternate request failures lack a complete reference. | Render form, list, conversion, and expired-key recovery states. |
| Long exact results need a defined fit rule. | Wrap the full number and retain every decimal digit. |
| Native date inputs can zoom on phones. | Keep input text at least 16px. |

## Rejected findings

| Proposed change | Reason for rejection |
| --- | --- |
| Remove eyebrow labels and the quiet ambient mesh. | The approved family explicitly includes these details. They support the worksheet hierarchy. |
| Replace the large result with ordinary body text. | The amount is the task output. Its emphasis serves the product. |
| Replace the two explanatory panels with a new visual concept. | The approved homepage already settles this composition. The receipt and list provide distinct layouts. |
| Introduce a new font or dark palette. | The item freezes typography and supplies only the approved light family. |
| Generate new Stitch screens or a new OpenDesign project. | The item requires adoption of the existing Currency Converter project. |
| Add image assets for visual interest. | The worksheet contains no placeholder photography or image workflow. |
| Copy the prototype financial service into production. | The API and Java library already own conversion behavior. |

## Review boundaries

The screenshots show design-only sample values and request states.
The checks prove mockup layout, target sizes, automated accessibility, keyboard focus, and reduced-motion behavior.
They do not prove production API integration, server behavior, CI, deployment, or application E2E results.
The implementer must reconcile existing UI against the completed design specification.
The reviewer must compare the implementation with these rendered references.
