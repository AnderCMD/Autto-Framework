#!/usr/bin/env bash
# Prepares the dev container: .env from the template, a browser for headless runs and a first build.
set -euo pipefail
cd "$(dirname "$0")/.."

[ -f .env ] || cp .env.example .env

# Chromium + driver for local headless runs inside the container (arm64 and amd64)
sudo apt-get update -qq
sudo apt-get install -y -qq chromium chromium-driver >/dev/null || echo "Chromium could not be installed: use the docker target instead"

./mvnw -q install -DskipTests
./scripts/doctor.sh || true
echo
echo "Ready. Edit .env, then: ./mvnw -pl autto-e2e test -Dcucumber.filter.tags=@smoke"
