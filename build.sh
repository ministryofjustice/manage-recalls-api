#!/bin/bash

set -euo pipefail

docker-compose up -d postgres

./gradlew ktlintFormat
./gradlew clean check
./gradlew verifyPactAndPublish
./scripts/start-gotenburg.sh
./gradlew documentGenerationTest
./scripts/stop-gotenburg.sh
