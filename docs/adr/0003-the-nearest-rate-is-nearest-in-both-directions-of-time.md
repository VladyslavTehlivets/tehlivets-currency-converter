# 0003 · The nearest rate is nearest in both directions of time

Status: accepted · 2026-11-09 · item tehlivets-currency-converter-1 · author: architect role

## Context

The item says: keep nearest-date selection. The design says: select the available rate nearest to the conversion date.
`ExchangeRates.getExchangeRate` takes the minimum of a signed `Duration.between(rateDate, requested).toNanos()`.
Defects found:
- A rate dated after the requested moment gives a negative distance. The furthest future rate wins.
- `toNanos` throws `ArithmeticException` when two moments are about 292 years apart.
- The existing test passes because every rate in it is in the past.
The result view also must show each leg and its selected rate. `convert` returns only the final `Money`.

## Options

| Option | Buys | Costs |
|---|---|---|
| (a) Fix the library: absolute distance, `Duration` comparison, earlier rate wins a tie. Add `explain` that returns the legs. | One rule, in the domain, for every client. | Changes library behaviour when a future rate exists. |
| (b) Keep the library. Choose the rate in the API layer. | Library untouched. | Two selection rules. Domain logic leaks into the adapter. |
| (c) Keep the defect. | No change. | The "How it works" view would state a false rule. |

## Decision

Choose (a).
- Distance is `Duration.between(rate, requested).abs()`. Compare `Duration` values. Never call `toNanos`.
- When two rates are equally near, the earlier-dated rate wins.
- `CurrencyConverter.explain(Money, Currency, Instant)` returns a `Conversion` with one or two `ConversionLeg` values.
- `convert` returns `explain(...).result()`. Its signature stays.
- `NotFoundExchangeRateException` names the missing direction and the requested moment.
- `Currency` exposes its code.
- The four existing tests stay unchanged and green.

## Deciding factor

The views explain the rule to a person. Only one rule in one place keeps that explanation true.

## Charter drivers

- Serves: Pragmatism (smallest fix at the root), Clients (one rule for every client).
- Trades away: strict behaviour compatibility for callers that relied on a future-dated rate. No such caller exists in the repository.
- Scale target: selection stays O(n) over at most 500 rates of one direction.

## Consequences

- New library tests pin the future-rate case, the tie, and the overflow case.
- Same-currency conversion stays as it is in the library. The API refuses it with `same_currency` before the library runs.
