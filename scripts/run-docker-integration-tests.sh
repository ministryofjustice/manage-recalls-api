#!/bin/bash

set -euo pipefail

readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly PROJECT_DIR="${SCRIPT_DIR}/.."
readonly DOCKER_COMPOSE_FILE="docker-compose-test.yml"
readonly DOCKER_COMPOSE_INT_TESTS_FILE="docker-compose-ci-tests.yml"

pushd ${PROJECT_DIR}
docker compose -f "${DOCKER_COMPOSE_FILE}" -f "${DOCKER_COMPOSE_INT_TESTS_FILE}" build
docker compose -f "${DOCKER_COMPOSE_FILE}" -f "${DOCKER_COMPOSE_INT_TESTS_FILE}" pull
docker compose -f "${DOCKER_COMPOSE_FILE}" -f "${DOCKER_COMPOSE_INT_TESTS_FILE}" up --exit-code-from manage-recalls-api-int-tests
popd
