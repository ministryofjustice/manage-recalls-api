#!/bin/bash
set -euo pipefail

function usage {
  echo "
./$(basename $0) [option]

Options:
    -h --> show usage
    -e --> environment (REQUIRED) - allowed values: 'dev' or 'preprod'
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
check_dep "kubectl" "asdf install kubectl 1.21.9"

# get cli options
while getopts :e:h opt; do
  case ${opt} in
  e) ENV=${OPTARG} ;;
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

# check for the ENV variable
set +u
if [[ ! "${ENV}" =~ ^(dev|preprod)$ ]]; then
  usage
  exit 1
fi
set -u

K8S_NAMESPACE="manage-recalls-${ENV}"
PFP_NAME="manage-recalls-api-redis-proxy-$(whoami | tr ._ -)"
SECRET=manage-recalls-api-redis

REDIS_HOST=$(kubectl -n "${K8S_NAMESPACE}" get secret "${SECRET}" -o json | jq -r '.data.primary_endpoint_address | @base64d')
REDIS_PASS=$(kubectl -n "${K8S_NAMESPACE}" get secret "${SECRET}" -o json | jq -r '.data.auth_token | @base64d')
LOCAL_PORT=6389
REDIS_COMMANDER_PORT=8081

echo "Connecting to ${K8S_NAMESPACE}"

set +e
PFP_COUNT=$(kubectl -n "${K8S_NAMESPACE}" get pods | grep -c "${PFP_NAME}")
set -e

if [ "${PFP_COUNT}" -eq "0" ]; then
  echo "Starting up the redis-commander pod..."

  kubectl -n "${K8S_NAMESPACE}" run \
    "${PFP_NAME}" \
    --image=rediscommander/redis-commander \
    --port="${REDIS_COMMANDER_PORT}" \
    --env="REDIS_HOST=${REDIS_HOST}" \
    --env="REDIS_PASSWORD=${REDIS_PASS}" \
    --env="REDIS_TLS=true"

  sleep 10
fi

set +e
cmd="kubectl -n ${K8S_NAMESPACE} port-forward ${PFP_NAME} ${LOCAL_PORT}:${REDIS_COMMANDER_PORT}"
pid=$(pgrep -f "${cmd}")
set -e

echo "You can now connect to redis-commander on http://127.0.0.1:${LOCAL_PORT}"

# if the port-forward isn't already running, start it...
if [[ -z "${pid}" ]]; then
  ${cmd}
fi
