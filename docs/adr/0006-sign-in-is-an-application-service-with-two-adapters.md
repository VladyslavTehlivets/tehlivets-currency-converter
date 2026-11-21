# 0006 · Sign-in is an application service with two adapters

Status: superseded by ADR 0008 and ADR 0009 · 2026-11-19 · item tehlivets-currency-converter-4 · author: architect role

## Context

ADR 0005 declares Google as the one door for everyone. It leaves the shape of the service, the refusal contract, and the adapters to this ADR. The factory standard (ADR-050) requires an application service `signIn(assertion)` that returns a session, a web redirect adapter, and `POST /v1/sessions` for native clients.

## Decision

- `SignInService.signIn(assertion)` is the only entry point. It returns a `Session` or a `Refusal`.
- Assertions: `GoogleAssertion(idToken)`, `GitHubAssertion(code, redirectUri)`, `TestPasswordAssertion(email, password)` (dev only).
- Web adapter: `/auth/google/start`, `/auth/google/callback`. State, nonce, and PKCE verifier live in one host-only cookie: HttpOnly, Secure, SameSite=Lax, ten minutes, read once.
- Native adapter: `POST /v1/sessions` with a short-lived server-issued nonce, read once.
- Identify members by `(provider, subject)`. Never by email or login.
- Link only through a provider-verified email. Unique `(provider, subject)`.
- Never store provider tokens. Exchange GitHub codes on the server only. Never accept a client GitHub token.
- Google ID tokens: verify against JWKS. Check iss (exactly `https://accounts.google.com` or `accounts.google.com`), aud, exp, nonce. Cache JWKS per isolate and honour Cache-Control.
- A token taken from the Google token endpoint over TLS can be read without its signature. A client-sent token is verified against JWKS.
- Refuse a token whose aud names another environment's client. Each environment has its own client.
- Refusal codes: cancelled, state-mismatch, token-for-another-client, email-unverified, not-invited, provider-unavailable. Returned as `?refused=<code>&door=<provider>`. No free text or email.
- The next path starts with one slash, never two slashes or a slash and a backslash. Carry it in the binder cookie.
- Issue a fresh session id in a `__Host-` cookie marked HttpOnly and Secure.
- Production serves no password form. The test door signs in only where the dev flag is set.

## Consequences

- New tables: `members`, `provider_identities`, `sessions`, `sign_in_nonces`.
- JWKS cache per isolate.
- Refusal contract documented in docs/api.md.
- Dev-only test door route exists only when `E2E_TEST_IDENTITY=on`.

## Drivers

Serve charter v1 Clients (load-bearing `/v1` contract, native reuse), Scale (one unique-index read, JWKS cached per isolate), Cost (free providers), Pragmatism (reuse proven code). Trade away password sign-in in production. Scale target: 100,000 members, path to 1,000,000.
