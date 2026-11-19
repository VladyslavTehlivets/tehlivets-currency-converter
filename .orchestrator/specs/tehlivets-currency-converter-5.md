# tehlivets-currency-converter-5: dev environment is down

## Symptom
The probe of `http://10.13.13.4:7070/` failed three times. The error is `fetch failed`. The rationale says NXDOMAIN.

## DNS
The host is an IP address, not a name. The address 10.13.13.4 is the `wg0` address of this machine.
Public DNS returns NXDOMAIN for the string `10.13.13.4`. That is expected and is not the fault.
The probe wrongly reads it as a missing record. No DNS change helps. No zone is involved.

## TLS
Not applicable. The URL uses plain HTTP.

## Proxy
Not applicable. No nginx vhost exists for this environment. The `vps-infrastructure` repository does not own it.

## Service
First broken layer. `curl` gets `Connection refused` on 10.13.13.4:7070. No process listens on port 7070.
The API jar and the web export are not built. No container or systemd unit runs the app.
`docker-compose.yml` defines only `postgres`. The repository has no Dockerfile and no unit file.

## Deploy
`ci.yml` is the only workflow. The last five runs are green. No workflow deploys or starts the dev environment.

## Cause
Nothing runs the application on 10.13.13.4:7070. The repository defines no way to start it as a dev environment.
The NXDOMAIN is a false signal from an IP-literal host.

## Fix
Code or configuration. The implementer opens a reviewed pull request with these changes:
1. Add `Dockerfile` at the root. Build the web export, then the API jar. Run `java -jar worksheet-api.jar` with `PORT=7070`.
2. Extend `docker-compose.yml` with an `api` service. Use `depends_on: postgres: condition: service_healthy`.
   Set `DATABASE_URL=postgresql://worksheets:worksheets@postgres:5432/worksheets`. Publish `10.13.13.4:7070:7070`.
   Add `restart: unless-stopped` and a healthcheck on `/`.
3. Document the dev start command `docker compose up -d --build --wait` in `README.md` and `CLAUDE.md`.
4. Run the command on this machine once the pull request merges. A fresh probe must show the worksheet page.
5. Check the `/` response names the project, not a placeholder.

Optional follow-up for the factory owner: the probe must skip the DNS check for IP-literal hosts.
No action by Vlad is needed. No DNS or VPS change applies.
