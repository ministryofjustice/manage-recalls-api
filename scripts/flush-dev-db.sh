#!/bin/bash
set -euo pipefail

function check_dep {
  if ! command -v "${1}" &>/dev/null; then
    echo "You need '${1}' - '${2}'"
    exit 1
  fi
}

# Check dependencies
check_dep "psql" "brew install postgresql"
check_dep "jq" "brew install jq"
check_dep "aws" "brew install awscli"
check_dep "kubectl" "asdf install kubectl 1.19.15"

# check environment
set +u
if [[ -z "${ENV}" ]]; then
  echo "You must set the ENV environment variable - allowed values: [dev, preprod, prod]"
  exit 1
fi
set -u

# kick off port forward
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
# shellcheck disable=SC1091
. "${SCRIPT_DIR}/port-forward-db.sh" &
sleep 2

until pg_isready -q -h "127.0.0.1" -p 5432; do
  echo "Postgres is unavailable - waiting..."
  sleep 1
done

##
## Clear the database...
##

# db connection details
ENVIRONMENT="ppud-replacement-${ENV}"
SECRET=manage-recalls-database
DB_NAME=$(kubectl -n "${ENVIRONMENT}" get secret "${SECRET}" -o json | jq -r '.data.name | @base64d')
DB_USER=$(kubectl -n "${ENVIRONMENT}" get secret "${SECRET}" -o json | jq -r '.data.username | @base64d')
DB_PASS=$(kubectl -n "${ENVIRONMENT}" get secret "${SECRET}" -o json | jq -r '.data.password | @base64d')
DB_PORT=5432

# run the things
export PGPASSWORD=${DB_PASS}
function run_psql_cmd() {
  psql \
    --username "${DB_USER}" \
    --host 127.0.0.1 \
    --port "${DB_PORT}" \
    --dbname "${DB_NAME}" \
    --command "${1}"

  echo "done."
}

echo "Clearing recall table"
run_psql_cmd "TRUNCATE recall CASCADE"
