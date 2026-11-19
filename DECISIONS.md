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

## 2026-11-20 · designer role · tehlivets-currency-converter-3

| Decision | Why | Record |
|---|---|---|
| The sign-in page mirrors the converter hero, with the door in a `Bezel`. | Every Soft R6 work surface sits in a bezel. The product is light only. | Design, Screens |
| The Google values enter `DESIGN.md` as one `provider-door` token group. | Google fixes its button colours. Only `ProviderDoor` may use them. | `DESIGN.md`, Provider door |
| The footer link is the only entry point. There is no signed-in indicator. | Nothing needs an account yet. The session cookie is HttpOnly. | Design, Not doing |
| The test door loads only after `GET /auth/test/door` returns 200. | One static export serves dev and production. Production downloads no test door code. | Design, States |
