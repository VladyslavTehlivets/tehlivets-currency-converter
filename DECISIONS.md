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
