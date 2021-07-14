#!/bin/bash

set -euo pipefail

./gradlew ktlintFormat
./gradlew clean check
./gradlew verifyPactAndPublish
./scripts/start-gotenburg.sh
./gradlew documentGenerationTest
./scripts/stop-gotenburg.sh
