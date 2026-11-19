# ADR 000: What the charter's four drivers mean for tehlivets-currency-converter

Status: accepted, 2026-11-19, item tehlivets-currency-converter-3. Charter version 1. Every later ADR cites this file in its drivers section.

## Context

The charter asks for 100,000 users from the first release. The product today is a single-container worksheet API with a static web export and Postgres. No native client exists yet.

## 1. Scale

Sized to 100,000 users. The process is stateless. Rates and worksheets live in Postgres. Convert reads at most two directions through an index. No in-memory cache. Any instance can restart without losing work. The path to 1,000,000 is open because state is external.

## 2. Clients

The web app is the first client. Native iOS and Android come later. The `/v1` contract is load-bearing and documented in `docs/api.md`. All domain logic stays behind the API. The web client computes nothing.

## 3. Cost

Idle infrastructure cost stays near zero. One small container. Neon free tier. Static export served by the same process. No subscriptions. Google and GitHub sign-in are free at any volume.

## 4. Pragmatism

Reuse before building. The conversion library stays as is. The yana design system ports to Soft Family R6. Archetypes guide the model. Measure before optimising.

## Trade away

Multi-region deployment. A second database read replica. Live rate providers. Accounts with passwords in production.

## Decision

The product answers to the charter by staying stateless, externalising state, keeping one versioned API, and reusing the library. It trades away multi-region and password accounts.
