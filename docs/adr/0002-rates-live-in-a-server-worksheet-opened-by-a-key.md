# 0002 · Rates live in a server worksheet that a key opens

Status: accepted · 2026-11-09 · item tehlivets-currency-converter-1 · author: architect role

## Context

The design has a "Your rates" view and an "Add a dated rate" view. Rates must stay editable and directional.
The product has no accounts and no sign-in design. The item forbids prototype fake authentication and local mock state as production services.
The charter asks for externalised state, per-route authorization, and native clients later.

## Options

| Option | Buys | Costs | Hard to reverse |
|---|---|---|---|
| (a) A server worksheet in Postgres. A random worksheet key opens it. The client keeps the key. | Durable rates. One API for web and native. No accounts. | A database, and a key a person can lose. | Low. Accounts can claim a worksheet later. |
| (b) Rates stay in browser storage. Each conversion request carries them. | No database. Zero storage cost. | Reads as local mock state. No rates resource for native clients. | Medium. |
| (c) Accounts with sign-in. | Cross-device rates. | No approved design. Large scope. Invents authentication. | High. |

## Decision

Choose (a).
- `POST /v1/worksheets` returns a 256-bit random key once. The server stores only its SHA-256 hash.
- Every `/v1/worksheet/...` route needs `Authorization: Bearer <worksheet key>`. A missing or unknown key gets 401.
- A rate id from another worksheet gets 404. The query always filters by the worksheet id.
- The web client creates the worksheet when the person keeps a first rate, never on page view.
- The web client keeps the key in `localStorage` behind one module, `worksheet-key.ts`.
- Use Neon Postgres (free tier). Request `DATABASE_URL` from the secret-keeper.

## Deciding factor

Only (a) stores real rates behind the API without inventing authentication.

## Limits

- A worksheet holds at most 500 dated rates. The add use case locks the worksheet row, then counts.
- One rate per direction and moment. A duplicate gets 409.
- Worksheet creation needs an edge rate-limit rule on `POST /v1/worksheets`. The delivery process configures it.

## Charter drivers

- Serves: Scale (state outside the process, indexed reads), Clients (rates are an API resource), Cost (Neon free tier, idle near zero).
- Trades away: user convenience across devices. A person on a second device starts a new worksheet.
- Scale target: 100,000 worksheets × 500 rates = 50 million rows at the cap. Reads use the index `(worksheet_id, source_currency, target_currency)`.

## Consequences

- A lost key loses access to the worksheet. The rates hold example data only, so the loss is small.
- Worksheet sharing, expiry, and export are out of scope.
