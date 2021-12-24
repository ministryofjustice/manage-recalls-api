#!/bin/bash

# With a valid hmpps-auth client_id and client_secret this script will create a temporary token for use
# as a `Bearer` token via e.g. Postman or Swagger.
# See also https://github.com/ministryofjustice/hmpps-auth/blob/main/README.md
# Note that these "client credentials" are not the same as user (login) credentials and are separately administered
# including e.g. associated ROLES.
# For use "as is" - with credentials from specific env vars - with env=dev, then set the following in the calling shell:
# export HMPPS_AUTH_CLIENT_ID
# export HMPPS_AUTH_CLIENT_SECRET_dev
# Note that history expansion (!) characters will need to be escaped within e.g. the password value
# E.g.:
# export HMPPS_AUTH_CLIENT_ID=your-client-id
# export HMPPS_AUTH_CLIENT_SECRET_dev="*<lw-etc-etc-etc\!NY/p+qc'Ey"
# Be careful about where you store or share any such credentials!

set -euo pipefail

ENV=${1-dev}
echo "Env set as $ENV"

readonly HMPPS_AUTH_URL=https://sign-in-$ENV.hmpps.service.justice.gov.uk
SECRET_VAR=HMPPS_AUTH_CLIENT_SECRET_${ENV}

readonly client_id=${HMPPS_AUTH_CLIENT_ID}
readonly client_secret=${!SECRET_VAR}

readonly access_token=$(curl -s -X POST "${HMPPS_AUTH_URL}/auth/oauth/token?grant_type=client_credentials" \
  -H 'Content-Type: application/json' \
  -H "Authorization: Basic $(echo -n ${client_id}:${client_secret} | base64)" | jq -r .access_token)

echo $access_token
