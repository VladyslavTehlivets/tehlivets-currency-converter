# ADR 0005: People sign in through Google or GitHub

Status: accepted, 2026-11-19, tehlivets-currency-converter-3. Cites ADR-050 of project-orchestrator and `docs/adr/000-charter-drivers.md`.

## Context

The product has no accounts today. Worksheets are opened by a random key. The factory standard (ADR-050) now requires Google or GitHub as the door. The audience is `everyone`, so Google is the one door. GitHub is not declared. No kept door exists.

## Decision

- People sign in with Google only. The audience is `everyone`.
- Sign-in is one application service `signIn(assertion)` that returns a session.
- The web redirect is one adapter. Native clients use `POST /v1/sessions`.
- Identify members by provider and subject: Google `sub`. Never use email or login.
- Link only through an email the provider verified (`email_verified`).
- One provider identity belongs to at most one member. Enforce with `UNIQUE (provider, subject)`.
- Never store provider tokens. Never accept a client token as proof. Exchange codes on the server.
- Use Google OIDC with PKCE S256. Keep state, nonce, and verifier in a host-only cookie marked HttpOnly, Secure, SameSite=Lax, ten minutes, read once.
- The next path must start with one slash.
- Issue a fresh session id in a `__Host-` cookie marked HttpOnly and Secure.
- Cache the Google JWKS per isolate and honour Cache-Control. Check iss, aud, exp, and nonce on every ID token.
- The Google iss is exactly `https://accounts.google.com` or `accounts.google.com`.
- A token taken from the Google token endpoint over TLS can be read without its signature. A client-sent token is verified against the Google JWKS.
- A Google token minted for another client is refused.
- Production serves no password form. The dev test door signs in only where the dev flag is set.
- The dev test identity stays `factory-e2e@currency.vtehsol.com`.

## Consequences

- A `members` table and a `provider_identities` table are added.
- A `sessions` table or signed session cookie holds the session.
- Worksheets stay keyed by their existing key. Ownership by member is out of scope.
- The declaration lives in `.orchestrator/sign-in.json`.

## Drivers

Serve charter v1 Clients: the API contract stays load-bearing for native clients. Serve Scale: every lookup is one unique-index read. Serve Cost: Google sign-in is free. Serve Pragmatism: reuse sessions and `jose`. Trade away the password door in production. Scale target: 100,000 members, path to 1,000,000.
