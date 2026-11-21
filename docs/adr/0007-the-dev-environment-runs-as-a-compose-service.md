# 0007 · The dev environment runs as a compose service

Status: accepted · 2026-11-21 · item tehlivets-currency-converter-5 · author: implementer role

## Context

The factory probes the dev environment at `http://10.13.13.4:7070/`.
The address 10.13.13.4 is the `wg0` address of the factory machine.
Nothing ran the application there. The repository had no Dockerfile and no service for the API.
The probe reported NXDOMAIN. That signal is false, because the host is an IP literal.
ADR 0001 left the host open. This ADR fills that gap for dev only.

## Options

| Option | Buys | Costs |
|---|---|---|
| (a) A Dockerfile and an `api` service in `docker-compose.yml`. | One command starts store and API. Docker restarts it. | One image build on the machine. |
| (b) A systemd unit that runs the jar on the host. | No image. | The unit lives outside the repository. The build must run on the host first. |
| (c) A cloud host. | Public reach. | A provider, credentials, and cost that no item asks for. |

## Decision

Choose (a).
The Dockerfile builds the web export, then the API jar, and runs the jar on a JRE as `nobody`.
The `api` service publishes `10.13.13.4:7070`. It waits until `postgres` is healthy.
It reports healthy only when `/` answers with `Currency Converter`. A placeholder page fails the check.
Start it with `docker compose up -d --build --wait`.
The image build uses the host network. Container DNS on the bridge network times out on the factory machine.

## Deciding factor

Only (a) keeps the whole start path in the repository and under review.

## Consequences

- `web/src/dev-environment.test.ts` pins the descriptor. A changed address, port, store, or health check turns it red.
- Postgres stays on loopback port 5446. The e2e server and local runs keep working.
- `/v1/health` reports revision `local` in this container. The image does not carry its commit yet.
- Known limit: at boot, Docker can start the container before `wg0` holds 10.13.13.4. The port bind then fails, and `restart: unless-stopped` does not retry it. Run `docker compose up -d --wait` again after a reboot. A reconciler like yana ADR 050 can fix this later.

## Reuse

From `pipeline/reuse-registry.md`:
- Readiness speaks the protocol. The health check reads the page, not a TCP connect.
- The boot race on a VPN address. This ADR names it as a known limit.
- The stamp travels with the code. Not ported yet, so the revision stays `local`.
