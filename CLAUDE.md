# Currency Converter

Currency Converter turns an amount into another currency with dated rates that you keep. It uses exact decimals and routes every cross conversion through PLN. It does not supply live quotes.

## Modules

| Module | Purpose |
|---|---|
| `conversion/` | Java library. Nearest dated rate, PLN routing, exact decimal legs, missing-rate errors. |
| `worksheet-api/` | Java API `/v1`. Worksheets, dated rates, conversions, Postgres storage. |
| `web/` | Next.js static export in Soft Family R6. Nine views. Served by the API process. |

## Build and test

| Suite | Command |
|---|---|
| Java library and API, with Postgres running | `./mvnw -B verify` |
| Web typecheck, unit tests, build | `cd web && npm run typecheck && npm test && npm run build` |
| End to end at 390, 768, and 1440 px | `cd web && npx playwright test` |

Start Postgres first: `docker compose up -d --wait postgres`.

## Run locally

1. Start Postgres: `docker compose up -d --wait postgres`
2. Build the API: `./mvnw -B -DskipTests package`
3. Build the web export: `cd web && npm ci && npm run build`
4. Start the API: `DATABASE_URL=postgresql://worksheets:worksheets@127.0.0.1:5446/worksheets java -jar worksheet-api/target/worksheet-api.jar`
5. Open `http://127.0.0.1:7070`.

## Conventions

- Java 21. Lombok for value objects. No comments in code.
- Compact Simplified Technical English for all documents (ADR-037).
- Do not hand-edit `docs/architecture.md`. It is generated.
- API contract lives in `docs/api.md`. Keep it current.
- The web client calls the API only through `web/src/lib/worksheet-api.ts`.

## Architecture

See the Architecture section in `README.md` and the ADRs in `docs/adr`.
