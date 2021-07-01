#!/bin/bash

set -euo pipefail

./gradlew ktlintFormat
./gradlew clean check
./scripts/start-gotenburg.sh
./gradlew documentGenerationTest
./scripts/stop-gotenburg.sh
