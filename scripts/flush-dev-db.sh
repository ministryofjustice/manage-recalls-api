#!/bin/bash

set -euo pipefail

# kick off port forward
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
bash "${SCRIPT_DIR}/port-forward-db.sh" &
sleep 5

# db connection details
ENVIRONMENT="ppud-replacement-dev"
SECRET=manage-recalls-database
DB_HOST=$(kubectl -n "${ENVIRONMENT}" get secret "${SECRET}" -o json | jq -r '.data.host | @base64d')
DB_NAME=$(kubectl -n "${ENVIRONMENT}" get secret "${SECRET}" -o json | jq -r '.data.name | @base64d')
DB_USER=$(kubectl -n "${ENVIRONMENT}" get secret "${SECRET}" -o json | jq -r '.data.username | @base64d')
DB_PASS=$(kubectl -n "${ENVIRONMENT}" get secret "${SECRET}" -o json | jq -r '.data.password | @base64d')
DB_PORT=5432

# run the things
export PGPASSWORD=${DB_PASS}
function run_psql_cmd() {
  psql \
    --username "${DB_USER}" \
    --host "${DB_HOST}" \
    --port "${DB_PORT}" \
    --dbname "${DB_NAME}" \
    --command "${1}"

  echo "done."
}

echo "Clearing recall table"
run_psql_cmd "TRUNCATE recall CASCADE"
