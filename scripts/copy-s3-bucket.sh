#!/bin/bash
set -euo pipefail

function usage {
  echo "
./$(basename $0) [option]

Options:
    -h --> show usage
    -f --> FROM environment (REQUIRED) - allowed values are 'preprod' or 'prod'
    -t --> TO environment (REQUIRED) - allowed values are 'dev' or 'preprod'
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
check_dep "aws" "brew install awscli"

# get cli options
while getopts :f:t:h opt; do
  case ${opt} in
  f) FROM_ENV=${OPTARG} ;;
  t) TO_ENV=${OPTARG} ;;
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

# check for the ENV variables
set +u
if [[ ! "${FROM_ENV}" =~ ^(preprod|prod)$ ]]; then
  usage
  exit 1
fi
if [[ ! "${TO_ENV}" =~ ^(dev|preprod)$ ]]; then
  usage
  exit 1
fi
set -u

FROM_NAMESPACE="manage-recalls-${FROM_ENV}"
TO_NAMESPACE="manage-recalls-${TO_ENV}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
TMP_DIR=s3_content

function kill_proxies {
  echo "Closing any existing proxies..."
  set +e
  pkill kubectl
  set -e
}

function wait_for_proxy {
  until netcat -z 127.0.0.1 3128; do
    echo "HTTP proxy is unavailable - waiting..."
    sleep 1
  done
}

kill_proxies

##
## Export the FROM S3 bucket...
##

# shellcheck disable=SC1091,SC2086
${SCRIPT_DIR}/port-forward-http-proxy.sh -e "${FROM_ENV}" &
sleep 2
wait_for_proxy

FROM_S3_SECRET=manage-recalls-api-s3-bucket
AWS_ACCESS_KEY_ID=$(kubectl -n "${FROM_NAMESPACE}" get secret "${FROM_S3_SECRET}" -o json | jq -r '.data.access_key_id | @base64d')
AWS_SECRET_ACCESS_KEY=$(kubectl -n "${FROM_NAMESPACE}" get secret "${FROM_S3_SECRET}" -o json | jq -r '.data.secret_access_key | @base64d')
AWS_BUCKET=$(kubectl -n "${FROM_NAMESPACE}" get secret "${FROM_S3_SECRET}" -o json | jq -r '.data.bucket_name | @base64d')
export AWS_ACCESS_KEY_ID
export AWS_SECRET_ACCESS_KEY
export AWS_DEFAULT_REGION="eu-west-2"
export HTTP_PROXY=http://127.0.0.1:3128
export HTTPS_PROXY=http://127.0.0.1:3128

echo "Pulling down the contents of the source S3 bucket..."
mkdir -p "${TMP_DIR}"
aws s3 sync "s3://${AWS_BUCKET}/" "${TMP_DIR}/" --delete

kill_proxies

unset HTTP_PROXY
unset HTTPS_PROXY

##
## Import to the TO S3 bucket...
##

# shellcheck disable=SC1091,SC2086
${SCRIPT_DIR}/port-forward-http-proxy.sh -e "${TO_ENV}" &
sleep 2
wait_for_proxy

TO_S3_SECRET=manage-recalls-api-s3-bucket
AWS_ACCESS_KEY_ID=$(kubectl -n "${TO_NAMESPACE}" get secret "${TO_S3_SECRET}" -o json | jq -r '.data.access_key_id | @base64d')
AWS_SECRET_ACCESS_KEY=$(kubectl -n "${TO_NAMESPACE}" get secret "${TO_S3_SECRET}" -o json | jq -r '.data.secret_access_key | @base64d')
AWS_BUCKET=$(kubectl -n "${TO_NAMESPACE}" get secret "${TO_S3_SECRET}" -o json | jq -r '.data.bucket_name | @base64d')
export AWS_ACCESS_KEY_ID
export AWS_SECRET_ACCESS_KEY
export AWS_DEFAULT_REGION="eu-west-2"
export HTTP_PROXY=http://127.0.0.1:3128
export HTTPS_PROXY=http://127.0.0.1:3128

echo "Pushing the contents to the target S3 bucket..."
aws s3 sync "${TMP_DIR}/" "s3://${AWS_BUCKET}/" --delete

kill_proxies

unset HTTP_PROXY
unset HTTPS_PROXY

rm -rf "${TMP_DIR}"

echo "done."
