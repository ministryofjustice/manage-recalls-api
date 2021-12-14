#!/bin/bash

# This script gets the token that the 'manage-recalls-api' uses to connect to other HMPPS services.
# The token it generates can not be used to access the 'manage-recalls-api' endpoints directly.

set -euo pipefail

ENV=${1-dev}
echo "Env set as $ENV"

readonly SECRET_NAME=manage-recalls-api
readonly NAMESPACE=manage-recalls-$ENV
readonly HMPPS_AUTH_URL=https://sign-in-$ENV.hmpps.service.justice.gov.uk

kubectl config set-context --current --namespace=$NAMESPACE

readonly secrets=$(kubectl get secret ${SECRET_NAME} -o json)
readonly client_id=$(jq -r '.data.API_CLIENT_ID' <<<"${secrets}" | base64 -d)
readonly client_secret=$(jq -r '.data.API_CLIENT_SECRET' <<<"${secrets}" | base64 -d)
readonly access_token=$(curl -s -X POST "${HMPPS_AUTH_URL}/auth/oauth/token?grant_type=client_credentials" \
  -H 'Content-Type: application/json' \
  -H "Authorization: Basic $(echo -n ${client_id}:${client_secret} | base64)" | jq -r .access_token)

echo $access_token
