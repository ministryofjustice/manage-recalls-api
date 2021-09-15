#!/bin/bash

set -euo pipefail

if ! docker info > /dev/null 2>&1; then
  echo "Docker isn't running.  Please start docker and try again"
  exit 1
fi

docker-compose up -d postgres localstack

./gradlew ktlintFormat
./gradlew clean check
./gradlew verifyPactAndPublish
./scripts/start-gotenburg.sh
./gradlew documentGenerationTest
./scripts/stop-gotenburg.sh
