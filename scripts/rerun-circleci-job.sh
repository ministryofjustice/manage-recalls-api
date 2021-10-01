#!/bin/bash
set -euo pipefail

function usage {
  echo "
./$(basename $0) [option]

Options:
    -h --> show usage
    -w --> workflow id (REQUIRED) - you will find this in the circleci url
    -j --> job name (REQUIRED) - the name of the job (within the workflow) to re-run (i.e. deploy_dev)
    -t --> circleci auth token (REQUIRED) - your circleci personal auth token
  "
}

while getopts :w:j:t:h opt; do
  case ${opt} in
  w) WORKFLOW=${OPTARG} ;;
  j) JOB=${OPTARG} ;;
  t) TOKEN=${OPTARG} ;;
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

set +u
if [[ -z ${WORKFLOW} ]] || [[ -z ${JOB} ]] || [[ -z ${TOKEN} ]]; then
  usage
  exit 1
fi
set -u

JOB_ID=$(
  curl \
    --request GET \
    --url "https://circleci.com/api/v2/workflow/${WORKFLOW}/job" \
    --user "${TOKEN}:" \
    --header 'content-type: application/json' \
    --silent |
    jq --arg job_name "${JOB}" -r '.items[] | select(.name == $job_name) | .id'
)

curl \
  --request POST \
  --url "https://circleci.com/api/v2/workflow/${WORKFLOW}/rerun" \
  --user "${TOKEN}:" \
  --header 'content-type: application/json' \
  --silent \
  --data "{\"jobs\":[\"${JOB_ID}\"],\"from_failed\":false,\"sparse_tree\":false}"
