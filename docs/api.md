# Worksheet API `/v1`

The worksheet API serves the conversion library to the web client and to future native clients. One process serves `/v1` and the web export (ADR 0001).

## Authorization

A worksheet key opens one worksheet (ADR 0002). Send it on every worksheet route:

```
Authorization: Bearer <worksheetKey>
```

The server stores only `SHA-256(key)`. A missing, malformed, or unknown key gets `401 unauthorized`.

## Routes

| Route | Auth | Success | Refusals |
|---|---|---|---|
| `POST /v1/worksheets` | none | `201 {worksheetKey}`, `Cache-Control: no-store` | `429` at the edge |
| `GET /v1/worksheet/rates` | key | `200 {rates:[Rate], limit:500}` | `401` |
| `POST /v1/worksheet/rates` | key | `201 Rate` | `400` field code, `401`, `409 rate_exists`, `422 worksheet_full`, `413 body_too_large` |
| `DELETE /v1/worksheet/rates/{id}` | key | `204` | `401`, `404 rate_not_found` |
| `POST /v1/worksheet/conversions` | key | `200 Receipt` | `400` field code, `401`, `422 rate_missing`, `413 body_too_large` |
| `GET /v1/health` | none | `200 {status:"ok", revision}` | none |

An unknown path under `/v1` gets `404 not_found`.

## Bodies

Rate request: `{"from":"EUR","to":"PLN","rate":"4.2515","ratedAt":"2026-03-02T12:00:00+01:00"}`

Rate: `{"id":"<uuid>","from":"EUR","to":"PLN","rate":"4.2515","ratedAt":"2026-03-02T11:00:00Z"}`

Conversion request: `{"amount":"100","from":"EUR","to":"USD","at":"2026-03-02T12:00:00Z"}`

Receipt:

```json
{
  "amount": "100",
  "result": "116.108465",
  "base": "PLN",
  "at": "2026-03-02T12:00:00Z",
  "legs": [
    {"from":"EUR","to":"PLN","rate":"4.2515","ratedAt":"2026-03-02T12:00:00Z","amountIn":"100","amountOut":"425.15"},
    {"from":"PLN","to":"USD","rate":"0.2731","ratedAt":"2026-03-02T12:00:00Z","amountIn":"425.15","amountOut":"116.108465"}
  ]
}
```

A direct conversion has one leg. A cross conversion has two legs through PLN.

## Field rules

| Field | Rule | Refusal code |
|---|---|---|
| `amount` | JSON string. Matches `^[0-9]{1,15}(\.[0-9]{1,12})?$`. Greater than zero. | `invalid_amount` |
| `rate` | Same rule as `amount`. | `invalid_rate` |
| `from`, `to` | JSON string. An ISO 4217 code. | `invalid_currency` |
| `from`, `to` | Two different currencies. | `same_currency` on `to` |
| `ratedAt`, `at` | ISO-8601 with an offset. `ratedAt` is between 1900 and 2100. | `invalid_moment` |
| body | One JSON object, at most 4096 bytes. | `invalid_body`, `body_too_large` |

A JSON number for a decimal is refused. The server never rounds. Decimals return as plain strings without trailing zeros.

## Refusals

Field refusal: `{"error":{"code":"invalid_amount","field":"amount"}}`

Rule refusal: `{"error":{"code":"rate_exists"}}`

Missing rate: `{"error":{"code":"rate_missing","missing":{"from":"EUR","to":"PLN"},"at":"2026-03-02T12:00:00Z"}}`

Codes are stable. The body carries no prose. The client maps each code to its own copy.

## Rate selection

The conversion uses the nearest rate in each direction (ADR 0003). Nearest is absolute distance in time. The earlier rate wins a tie. A rate never supplies its reverse direction.

## Configuration

| Variable | Default | Use |
|---|---|---|
| `DATABASE_URL` | none | Postgres URL. Flyway runs the migrations at start. |
| `PORT` | `7070` | HTTP port. |
| `REVISION` | `local` | Reported by `/v1/health`. |
| `WEB_EXPORT_DIR` | `web/out` | Static web export. The server skips it when the directory is absent. |
| `GOOGLE_CLIENT_IDS` | none | Comma list of this environment's Google client IDs. Empty closes the door. |
| `GOOGLE_CLIENT_SECRET` | none | The client secret for the code exchange. |
| `PUBLIC_BASE_URL` | `http://127.0.0.1:7070` | Origin that Google returns to. The callback is `/auth/google/callback`. |
| `DEV_TEST_DOOR_EMAIL`, `DEV_TEST_DOOR_PASSWORD` | none | The dev test identity (ADR-019). Set both only in dev. Unset closes the test door. |
| `GOOGLE_TOKEN_ENDPOINT`, `GOOGLE_JWKS_URI` | Google's own | Test overrides. Leave unset in every real environment. |

## Sign in (ADR 0005)

People sign in with Google. Each environment has its own OAuth client. The server refuses a token whose `aud` names another client.

### Session cookie

A session sits in `__Host-session`: HttpOnly, Secure, SameSite=Lax, `Path=/`, 30 days. Every sign-in issues a fresh id.

### Routes

| Route | Auth | Success | Refusals |
|---|---|---|---|
| `POST /v1/sessions/nonce` | none | `201 {nonce}`. One use, five minutes. | none |
| `POST /v1/sessions` | none | `201 {session:{id,expiresAt}}` and the cookie | `401 {error:{code}}` with a closed code |
| `POST /auth/google/start` | none | `303` to Google. Form field `next`. Sets `__Host-sign-in` for ten minutes. | `303` to the sign-in page with a code |
| `GET /auth/google/callback` | binder cookie | `303` to `next`, with the session cookie | `303 /sign-in?refused=<code>&door=google` |
| `GET /auth/test/door` | none | `200` only where the dev test door is set | `404` |
| `POST /auth/test/sign-in` | none | `303` to `next`, with the session cookie. Form fields `email`, `password`, `next`. | `303` with `not-invited`, or `404` |

`POST /v1/sessions` takes `{"provider":"google","idToken":"<jwt>","nonce":"<from /v1/sessions/nonce>"}`. The server verifies the signature against the Google JWKS, then checks `iss`, `aud`, `exp`, and `nonce`. Native clients use it.

### Refusal codes

`cancelled`, `state-mismatch`, `token-for-another-client`, `email-unverified`, `not-invited`, `provider-unavailable`. A refusal never carries free text or an email address.

### Members

A member is a provider and a subject (Google `sub`). Email never names a member. A provider identity joins an existing member only through an email the provider marked verified. One identity belongs to one member (`UNIQUE (provider, subject)`).

`next` must start with one slash and never `//` or `/\`. Anything else becomes `/`.
