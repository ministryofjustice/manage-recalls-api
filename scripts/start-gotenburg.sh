#!/bin/bash

set -euo pipefail

readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly PROJECT_DIR="${SCRIPT_DIR}/.."
readonly MANAGE_RECALLS_SERVICE_NAME='manage-recalls-api'
readonly LOG_FILE="/tmp/${MANAGE_RECALLS_SERVICE_NAME}.log"
readonly DOCKER_COMPOSE_FILE="$PROJECT_DIR/docker-compose-test.yml"

echo "Starting gotenburg"
docker compose -f "${DOCKER_COMPOSE_FILE}" pull
docker compose -f "${DOCKER_COMPOSE_FILE}" up -d

#echo "Starting ${MANAGE_RECALLS_SERVICE_NAME}"
#SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun >> "${LOG_FILE}" 2>&1 &