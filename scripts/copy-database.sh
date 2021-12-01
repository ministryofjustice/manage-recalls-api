#!/bin/bash
set -euo pipefail

function usage {
  echo "
./$(basename $0) [option]

Options:
    -h --> show usage
    -f --> FROM environment (REQUIRED)
    -g --> FROM namespace prefix (REQUIRED)
    -t --> TO environment (REQUIRED)
    -u --> TO namespace prefix (REQUIRED)
  "
}

function check_dep {
  if ! command -v "${1}" &>/dev/null; then
    echo "You need '${1}' - '${2}'"
    exit 1
  fi
}

# Check dependencies
check_dep "jq" "brew install jq"
check_dep "kubectl" "asdf install kubectl 1.19.15"
check_dep "pg_dump" "brew install postgresql"
check_dep "pg_restore" "brew install postgresql"
check_dep "pg_isready" "brew install postgresql"

# get cli options
while getopts :f:g:t:u:h opt; do
  case ${opt} in
  f) FROM_ENV=${OPTARG} ;;
  g) FROM_NAMESPACE_PREFIX=${OPTARG} ;;
  t) TO_ENV=${OPTARG} ;;
  u) TO_NAMESPACE_PREFIX=${OPTARG} ;;
  h)
    usage
    exit
    ;;
  \?)
    echo "Unknown option: -${OPTARG}" >&2
    exit 1
    ;;
  :)
    echo "Missing option argument for -${OPTARG}" >&2
    exit 1
    ;;
  *)
    echo "Unimplemented option: -${OPTARG}" >&2
    exit 1
    ;;
  esac
done

FROM_NAMESPACE="${FROM_NAMESPACE_PREFIX}-${FROM_ENV}"
TO_NAMESPACE="${TO_NAMESPACE_PREFIX}-${TO_ENV}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
PGDUMP_FILE=manage_recalls_dump.sqlc

function kill_proxies {
  echo "Closing any existing proxies..."
  set +e
  pkill kubectl
  set -e
}

function wait_for_db {
  until pg_isready -q -h "127.0.0.1" -p 5432; do
    echo "DB proxy is unavailable - waiting..."
    sleep 1
  done
}

kill_proxies

##
## Export the FROM database...
##

# kick off db port forward
# shellcheck disable=SC1091,SC2086
${SCRIPT_DIR}/port-forward-db.sh -e "${FROM_ENV}" -n "${FROM_NAMESPACE_PREFIX}" &
sleep 5
wait_for_db

# db connection details
FROM_DB_SECRET=manage-recalls-database
if [ "${FROM_NAMESPACE_PREFIX}" == "manage-recalls" ]; then
  FROM_DB_SECRET=manage-recalls-api-database
fi

DB_NAME=$(kubectl -n "${FROM_NAMESPACE}" get secret "${FROM_DB_SECRET}" -o json | jq -r '.data.name | @base64d')
DB_USER=$(kubectl -n "${FROM_NAMESPACE}" get secret "${FROM_DB_SECRET}" -o json | jq -r '.data.username | @base64d')
DB_PASS=$(kubectl -n "${FROM_NAMESPACE}" get secret "${FROM_DB_SECRET}" -o json | jq -r '.data.password | @base64d')
DB_PORT=5432
export PGPASSWORD="${DB_PASS}"

echo "Running pg_dump on the source database ..."
rm -f "${PGDUMP_FILE}"
pg_dump \
  --host=127.0.0.1 \
  --port=${DB_PORT} \
  --username="${DB_USER}" \
  --format=c \
  --file=${PGDUMP_FILE} \
  --no-owner \
  --no-acl \
  --clean \
  --if-exists \
  "${DB_NAME}"

kill_proxies

##
## Import into the TO database...
##

# kick off db port forward
# shellcheck disable=SC1091,SC2086
${SCRIPT_DIR}/port-forward-db.sh -e "${TO_ENV}" -n "${TO_NAMESPACE_PREFIX}" &
sleep 5
wait_for_db

# db connection details
TO_DB_SECRET=manage-recalls-database
if [ "${TO_NAMESPACE_PREFIX}" == "manage-recalls" ]; then
  TO_DB_SECRET=manage-recalls-api-database
fi

DB_NAME=$(kubectl -n "${TO_NAMESPACE}" get secret "${TO_DB_SECRET}" -o json | jq -r '.data.name | @base64d')
DB_USER=$(kubectl -n "${TO_NAMESPACE}" get secret "${TO_DB_SECRET}" -o json | jq -r '.data.username | @base64d')
DB_PASS=$(kubectl -n "${TO_NAMESPACE}" get secret "${TO_DB_SECRET}" -o json | jq -r '.data.password | @base64d')
DB_PORT=5432
export PGPASSWORD="${DB_PASS}"

echo "Running pg_restore on the target database ..."
pg_restore \
  --host=127.0.0.1 \
  --port=${DB_PORT} \
  --username="${DB_USER}" \
  --dbname="${DB_NAME}" \
  --no-owner \
  --no-acl \
  --clean \
  --if-exists \
  --format=c \
  ${PGDUMP_FILE}

rm -f "${PGDUMP_FILE}"

kill_proxies

echo "done."
