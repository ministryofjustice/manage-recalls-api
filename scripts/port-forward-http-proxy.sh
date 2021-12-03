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
check_dep "aws" "brew install awscli"
check_dep "kubectl" "asdf install kubectl 1.19.15"

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
PROXY_NAME="manage-recalls-api-http-proxy-$(whoami | tr ._ -)"
PROXY_PORT=3128

echo "Connecting to ${K8S_NAMESPACE}"

set +e
PROXY_COUNT=$(kubectl -n "${K8S_NAMESPACE}" get pods | grep -c "${PROXY_NAME}")
set -e

if [ "${PROXY_COUNT}" -eq "0" ]; then
  echo "Starting up http proxy pod..."

  cat <<EOF | kubectl apply -n "${K8S_NAMESPACE}" -f -
---
apiVersion: v1
kind: Pod
metadata:
  name: ${PROXY_NAME}
spec:
  containers:
    - image: b4tman/squid
      name: squid
      ports:
        - containerPort: ${PROXY_PORT}
  securityContext:
    fsGroup: 3128
    runAsGroup: 3128
    runAsUser: 3128
EOF

  sleep 5
fi

set +e
cmd="kubectl -n ${K8S_NAMESPACE} port-forward ${PROXY_NAME} ${PROXY_PORT}:${PROXY_PORT}"
pid=$(pgrep -f "${cmd}")
set -e

echo "You can now set your HTTP_PROXY and HTTPS_PROXY env varable to http://127.0.0.1:${PROXY_PORT}"

# if the port-forward isn't already running, start it...
if [[ -z "${pid}" ]]; then
  ${cmd}
fi
