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
| `DATABASE_URL` | none | Postgres URL. Flyway runs `V1__worksheets.sql` at start. |
| `PORT` | `7070` | HTTP port. |
| `REVISION` | `local` | Reported by `/v1/health`. |
| `WEB_EXPORT_DIR` | `web/out` | Static web export. The server skips it when the directory is absent. |
