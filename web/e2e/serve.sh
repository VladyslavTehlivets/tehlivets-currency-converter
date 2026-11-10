#!/usr/bin/env bash
set -euo pipefail
repository_root="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$repository_root"
docker compose up -d --wait postgres >/dev/null
export DATABASE_URL="${DATABASE_URL:-postgresql://worksheets:worksheets@127.0.0.1:5446/worksheets}"
export PORT="${E2E_PORT:-7171}"
export REVISION="$(git rev-parse HEAD)"
export WEB_EXPORT_DIR="$repository_root/web/out"
exec java -jar worksheet-api/target/worksheet-api.jar
