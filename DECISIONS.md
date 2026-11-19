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

## 2026-11-20 · designer role · tehlivets-currency-converter-4

| Decision | Why | Record |
|---|---|---|
| `DESIGN.md` now lives at the repository root, copied from the approved R6 file. | ADR-054 names one root reference for every role. The item-1 copy stays as the approved snapshot. | `DESIGN.md` |
| The Google button values enter `DESIGN.md` as one provider door group. | Google fixes them. Only the door primitive can use them. | `DESIGN.md`, Provider door group |
| The sign-in door sits in the product bezel and hero grid, not a bare column. | `DESIGN.md` puts every work surface in a bezel. The product is light only. | Design, Not doing |
| Refusal notices use two tones and no icons. | The palette has no amber, and the product has no icon set. | Design, States |
| Dev on http shows no Google door and offers the test door. | Google cannot return to an http LAN address. | Design, States |
| No signed-in indicator or sign-out in this item. | The plan names no session read route. | Design, Not doing |
