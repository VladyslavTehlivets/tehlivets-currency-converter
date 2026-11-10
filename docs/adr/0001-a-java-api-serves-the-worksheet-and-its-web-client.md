# 0001 · A Java API serves the worksheet and its web client

Status: accepted · 2026-11-09 · item tehlivets-currency-converter-1 · author: architect role

## Context

The repository holds only a Java 8 conversion library and four JUnit tests.
No web application, deployment, or rate provider exists.
The item asks for a nine-view web worksheet around the existing Java conversion capabilities.
The charter asks for a versioned API, domain logic behind it, and native clients later.

## Options

| Option | Buys | Costs | Hard to reverse |
|---|---|---|---|
| (a) Java HTTP API wraps the library. One container serves `/v1` and the static web build. | One copy of the domain. Native clients use the same API. | Two languages: Java and TypeScript. | Low. The web build can move to a CDN later. |
| (b) Port the domain to TypeScript on Cloudflare Workers. | Free edge runtime. One language. | Two copies of the domain. Breaks "around the existing Java library". | High. The copies drift. |
| (c) Compile Java to WebAssembly and convert in the browser. | No server. | Domain logic in the client. No API for native clients. | High. |

## Decision

Choose (a).
Use Javalin 6 on Java 21. Use Jackson with decimals as JSON strings.
Serve the static web export from the same container under `/`.
Version the API under `/v1`. Document it in `docs/api.md`.
Restructure the build as a Maven aggregator with two modules: `conversion` (the library, moved with `git mv`) and `worksheet-api`.
Add the Maven wrapper, because the factory machine has no `mvn`.

## Deciding factor

Only (a) keeps one copy of the conversion rules and gives native clients the same contract.

## Charter drivers

- Serves: Clients (load-bearing `/v1` contract), Pragmatism (reuse the library as is), Scale (stateless process).
- Trades away: builder elegance (two languages), and a free edge runtime.
- Scale target: 100,000 users. The process holds no user state, so instances scale out behind one load balancer.

## Consequences

- The process is stateless. Rates live in Postgres (ADR 0002).
- The container needs a host. The delivery process chooses it. This ADR does not claim a deployment.
- Spring Boot was rejected: its start time and memory are larger than five routes need.
- Plain `com.sun.net.httpserver` was rejected: routing, JSON errors, and static files would be hand code.

## Reuse

No Java entry exists in `pipeline/reuse-registry.md`.
Static serving uses the Javalin static-files plugin. The project-orchestrator `built-assets.ts` rules (path stays inside the build, hashed assets immutable, entry page not cached) are the acceptance checks for it.
