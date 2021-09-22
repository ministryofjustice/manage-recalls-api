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

# check environment
set +u
if [[ -z "${ENV}" ]]; then
  echo "You must set the ENV environment variable - allowed values: [dev, preprod, prod]"
  exit 1
fi
set -u

ENVIRONMENT="ppud-replacement-${ENV}"
PFP_NAME="manage-recalls-api-port-forward-$(whoami | tr ._ -)"
SECRET=manage-recalls-database

DB_HOST=$(kubectl -n "${ENVIRONMENT}" get secret "${SECRET}" -o json | jq -r '.data.host | @base64d')
DB_USER=$(kubectl -n "${ENVIRONMENT}" get secret "${SECRET}" -o json | jq -r '.data.username | @base64d')
DB_PASS=$(kubectl -n "${ENVIRONMENT}" get secret "${SECRET}" -o json | jq -r '.data.password | @base64d')
DB_PORT=5432

echo "Connecting to ${ENVIRONMENT}"

set +e
PFP_COUNT=$(kubectl -n "${ENVIRONMENT}" get pods | grep -c "${PFP_NAME}")
set -e

if [ "${PFP_COUNT}" -eq "0" ]; then
  echo "Starting up port forward pod..."

  kubectl -n "${ENVIRONMENT}" run \
    "${PFP_NAME}" \
    --image=ministryofjustice/port-forward \
    --port="${DB_PORT}" \
    --env="REMOTE_HOST=${DB_HOST}" \
    --env="LOCAL_PORT=${DB_PORT}" \
    --env="REMOTE_PORT=${DB_PORT}"

  sleep 5
fi

set +e
cmd="kubectl -n ${ENVIRONMENT} port-forward ${PFP_NAME} ${DB_PORT}:${DB_PORT}"
pid=$(pgrep -f "${cmd}")
set -e

echo "You can now connect to the database on 127.0.0.1:${DB_PORT} - username: ${DB_USER} - password: ${DB_PASS}"

# if the port-forward isn't already running, start it...
if [[ -z "${pid}" ]]; then
  ${cmd}
fi
