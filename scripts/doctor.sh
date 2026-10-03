#!/usr/bin/env bash
# Environment self-check: JDK, configuration, secrets, browsers, Docker and reachability of the application.
set -euo pipefail
cd "$(dirname "$0")/.."
./mvnw -q -ntp -pl autto-core compile exec:java \
  -Dexec.mainClass=io.github.andercmd.autto.core.doctor.Doctor "$@"
