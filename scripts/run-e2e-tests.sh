#!/usr/bin/env bash

set -euo pipefail

function usage {
  echo "
./$(basename $0) [option]
Options:
    -h --> show usage
    -e --> environment (REQUIRED) - allowed values: 'dev' or 'preprod'
  "
}

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

echo "Triggering the workflow..."

BUILD_PARAMS=("{\"check-docker\": false, \"check-dev\": true, \"check-preprod\": false}")
if [ "${ENV}" == "preprod" ]; then
  BUILD_PARAMS=("{\"check-docker\": false, \"check-dev\": false, \"check-preprod\": true}")
fi

POST_BODY=("{\"branch\":\"main\", \"parameters\": ${BUILD_PARAMS[@]}}")
BUILD_RESPONSE=$(
  curl \
    --silent \
    --header "Circle-Token: ${CIRCLECI_AUTH_TOKEN}" \
    --request POST \
    --retry 5 \
    --connect-timeout 10 \
    --max-time 60 \
    --header 'Content-Type: application/json' \
    --data "${POST_BODY[@]}" \
    --url https://circleci.com/api/v2/project/github/ministryofjustice/manage-recalls-e2e-tests/pipeline
)

echo "Trigger response:"
echo "${BUILD_RESPONSE}"

BUILD_ID=$(echo "${BUILD_RESPONSE}" | jq -r '.id')

if [[ ${BUILD_ID} == null ]]; then
  echo "Unable to trigger workflow..."
  exit 1
fi

WAIT=1500
TIMEOUT=$(("${SECONDS}" + "${WAIT}"))
STOPPED_TIME=null
BUILD_URL="https://circleci.com/api/v2/pipeline/${BUILD_ID}/workflow"

echo "Waiting up to ${WAIT} seconds for the workflow to complete..."
echo "  - API Endpoint: ${BUILD_URL}"
sleep 10

while [ ${STOPPED_TIME} == "null" ] && [ ${SECONDS} -le ${TIMEOUT} ]; do
  STATUS_RESPONSE=$(
    curl \
      --silent \
      --header "Circle-Token: << parameters.token >>" \
      --request GET \
      --retry 5 \
      --connect-timeout 10 \
      --max-time 60 \
      --url "${BUILD_URL}"
  )

  STOPPED_TIME=$(echo "${STATUS_RESPONSE}" | jq -r '.items[0].stopped_at')
  STATUS=$(echo "${STATUS_RESPONSE}" | jq -r '.items[0].status')

  if [ "${STOPPED_TIME}" == "null" ]; then
    echo " - status: ${STATUS}"
    sleep 15
  fi
done

echo "Workflow complete - status: ${STATUS}"

if [ "${STATUS}" != "success" ]; then
  exit 1
fi
