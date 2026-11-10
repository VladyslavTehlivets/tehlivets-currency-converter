# Decisions

Autonomous engineering calls. Each one is the decision of the named role under the factory directives. None of them quotes Vlad.

## 2026-11-09 · architect role · tehlivets-currency-converter-1

| Decision | Why | Record |
|---|---|---|
| A Java API wraps the library and serves the web export. | One copy of the domain. Native clients use the same `/v1`. | ADR 0001 |
| Rates live in a server worksheet that a key opens. No accounts. | Real storage without fake authentication. | ADR 0002 |
| The nearest rate uses absolute distance. The earlier rate wins a tie. | The library picked the furthest future rate. | ADR 0003 |
| The web client ports the yana design system in Soft R6 materials. | Reuse a proven, guarded system. | ADR 0004 |
| No upload control ships. | R6.1 covers FamilyAssistant and Teplo. This product has no file workflow. | Plan, Not doing |
| The API refuses same-currency conversion. | The library fails or routes through PLN for it. | Plan, Domain model |

## 2026-11-10 · designer role · tehlivets-currency-converter-1

These decisions belong to the designer under the item directives.

| Decision | Reason |
| --- | --- |
| Reuse the existing OpenDesign project and all nine approved screens. | The item names the approved project and requires its preservation. |
| Skip another Stitch generation and a visual-world picker. | New generation can change the settled family and spend unnecessary quota. |
| Add the root design reference and the missing screen specification. | The branch contains production UI but lacks the required designer hand-off. |
| Correct small targets, receipt heading order, and incomplete state examples in design mockups. | Accessibility and recovery need explicit rendered references. |
| Reuse existing component boundaries and yana-derived primitives. | ADR 0004 and the reuse registry already settle the stack. |
| Keep the approved light family and typography. | The item defers further typography changes and provides no approved dark palette. |
| Store new rendered evidence in Cloudflare. | The brief requires off-git rendered artifacts. |
| Leave production code and delivery gates to their assigned roles. | This run owns only design-agreed. |
