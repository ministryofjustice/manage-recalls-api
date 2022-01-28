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

function num_deploy_only_workflow_running() {
  curl \
    --silent \
    --header "Circle-Token: ${CIRCLECI_AUTH_TOKEN}" \
    --retry 5 \
    --connect-timeout 10 \
    --max-time 60 \
    --header 'Content-Type: application/json' \
    "https://circleci.com/api/v1.1/project/github/ministryofjustice/manage-recalls-e2e-tests?shallow=true&limit=10" |
    jq -r '.[] | select(.workflows.workflow_name == "deploy_only" and .lifecycle == "running") | .workflows.workflow_id' |
    wc -l
}

function check_deploy_only_workflow_isnt_running() {
  echo "Checking to see if the 'deploy_only' workflow is already running."

  if [ "$(num_deploy_only_workflow_running)" -gt "0" ]; then
    echo "The workflow 'deploy_only' is currently running, will wait for it to complete before invoking."

    WAIT=500
    TIMEOUT=$(("${SECONDS}" + "${WAIT}"))

    echo "Waiting up to ${WAIT} seconds for the workflow to complete..."
    sleep 10

    while [ "$(num_deploy_only_workflow_running)" -gt "0" ] && [ ${SECONDS} -le ${TIMEOUT} ]; do
      echo " - waiting..."
      sleep 15
    done
  fi

  echo " - all clear, ready to go..."
}

function trigger_and_follow_workflow() {
  echo "Triggering the workflow..."

  BUILD_PARAMS=("{\"only_run_deploy_check\": true, \"e2e_environment\": \"${ENV}\"}")
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

  WAIT=500
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
}

check_deploy_only_workflow_isnt_running
trigger_and_follow_workflow
