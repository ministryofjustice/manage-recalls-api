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
check_dep "psql" "brew install postgresql"
check_dep "jq" "brew install jq"
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

K8S_NAMESPACE="ppud-replacement-${ENV}"

# kick off port forward
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
# shellcheck disable=SC1091
${SCRIPT_DIR}/port-forward-db.sh -e ${ENV} &
sleep 2

until pg_isready -q -h "127.0.0.1" -p 5432; do
  echo "Postgres is unavailable - waiting..."
  sleep 1
done

##
## Clear the database...
##

# db connection details
DB_SECRET=manage-recalls-database
DB_NAME=$(kubectl -n "${K8S_NAMESPACE}" get secret "${DB_SECRET}" -o json | jq -r '.data.name | @base64d')
DB_USER=$(kubectl -n "${K8S_NAMESPACE}" get secret "${DB_SECRET}" -o json | jq -r '.data.username | @base64d')
DB_PASS=$(kubectl -n "${K8S_NAMESPACE}" get secret "${DB_SECRET}" -o json | jq -r '.data.password | @base64d')
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
}

echo "Clearing the database..."
run_psql_cmd "TRUNCATE recall CASCADE"
echo "done."

##
## Clear the S3 bucket
##

# connection details
S3_SECRET=manage-recalls-s3-bucket
AWS_ACCESS_KEY_ID=$(kubectl -n "${K8S_NAMESPACE}" get secret "${S3_SECRET}" -o json | jq -r '.data.access_key_id | @base64d')
AWS_SECRET_ACCESS_KEY=$(kubectl -n "${K8S_NAMESPACE}" get secret "${S3_SECRET}" -o json | jq -r '.data.secret_access_key | @base64d')
AWS_BUCKET=$(kubectl -n "${K8S_NAMESPACE}" get secret "${S3_SECRET}" -o json | jq -r '.data.bucket_name | @base64d')
AWS_DEFAULT_REGION="eu-west-2"
export AWS_ACCESS_KEY_ID
export AWS_SECRET_ACCESS_KEY
export AWS_DEFAULT_REGION

echo "Emptying the S3 bucket..."
aws s3 rm "s3://${AWS_BUCKET}" --recursive
echo "done."
