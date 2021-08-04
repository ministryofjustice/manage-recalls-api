#!/bin/bash

set -euo pipefail

ENVIRONMENT="ppud-replacement-${ENV:-preprod}"
PFP_NAME="manage-recalls-api-port-forward-$(whoami)"
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

echo "You can now connect to the database on 127.0.0.1:${DB_PORT} - username: ${DB_USER} - password: ${DB_PASS}"

kubectl -n "${ENVIRONMENT}" port-forward "${PFP_NAME}" "${DB_PORT}:${DB_PORT}"
