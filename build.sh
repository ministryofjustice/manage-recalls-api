#!/bin/bash

set -euo pipefail

./gradlew ktlintFormat
./gradlew clean check
./scripts/run-docker-integration-tests.sh
