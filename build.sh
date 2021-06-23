#!/bin/bash

set -euo pipefail

./gradlew clean ktlintFormat check
./scripts/start-gotenburg.sh
./gradlew documentGenerationTest
./scripts/stop-gotenburg.sh