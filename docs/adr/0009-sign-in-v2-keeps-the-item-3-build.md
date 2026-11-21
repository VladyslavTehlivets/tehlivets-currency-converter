# ADR 0009: Sign-in v2 keeps the item 3 build

Status: accepted, 2026-11-21, tehlivets-currency-converter-4. Cites ADR-050 of project-orchestrator and `docs/adr/000-charter-drivers.md`.

## Context

Pipeline item sign-in v2 asks for the same standard as item 3. Item 3 merged first. It holds the Google door, `POST /v1/sessions`, the dev test door, and their tests. ADR 0008 records that build. Item 4 planned a second build in `pl.umcs.auth` with its own `V2` migration. The two builds clash on tables, routes, and class names.

## Decision

- Item 4 builds no second sign-in. The item 3 build on `base-functionality` is the sign-in of this product.
- ADR 0008 is the record of the sign-in design. ADR 0005 and ADR 0006 are superseded by it.
- `.orchestrator/sign-in.json` stays `everyone` with the `google` door and no kept door.
- The dev test identity of ADR-019 stays `factory-e2e@currency.vtehsol.com` on `/sign-in`, in dev only.
- Item 4 adds one guard that item 3 lacks. Only the provider door primitive may use the provider colours and the Google mark.

## Consequences

- One `members` schema, one set of routes, one door.
- The item 4 spec routes (`GET /auth/google/start`, `POST /v1/sessions/nonces`) differ from the build. `docs/api.md` states the built routes.
- A future GitHub door follows ADR 0008 and needs a new audience declaration.

## Drivers

Serve charter v1 Pragmatism: reuse the merged build. Serve Clients: one load-bearing `/v1` contract. Trade away a second, parallel implementation.
