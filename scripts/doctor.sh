#!/usr/bin/env bash
# Environment self-check: JDK, configuration, secrets, browsers, Docker and reachability of the application.
# It reads the configuration of the test suite (autto-e2e); requires autto-core installed (./mvnw install -DskipTests).
set -euo pipefail
cd "$(dirname "$0")/.."
./mvnw -q -ntp -pl autto-e2e test-compile exec:java \
  -Dexec.mainClass=io.github.andercmd.autto.core.doctor.Doctor \
  -Dexec.classpathScope=test "$@"
