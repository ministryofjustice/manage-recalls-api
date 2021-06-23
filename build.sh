#!/bin/bash

set -euo pipefail

./gradlew clean check
./gradlew ktlintFormat
./scripts/start-gotenburg.sh
./gradlew documentGenerationTest
./scripts/stop-gotenburg.sh