#!/usr/bin/env bash
# Runs the end-to-end suite and, if scenarios fail, runs ONLY those once more ("rerun pass").
#
#   ./scripts/run-with-rerun.sh -Dcucumber.filter.tags=@smoke -Dautto.browser.headless=true
#
# Why: a scenario that fails and then passes is flaky. It must not turn the build red for no reason, but it must not
# stay invisible either: the rerun pass writes its own report (autto-e2e/target/autto-reports-rerun, scenarios tagged
# "rerun") and metrics.json counts them in "recovered_on_rerun". Scenarios that fail twice fail the build.
#
# Requires autto-core to be installed (./mvnw install -DskipTests) or built in the same reactor.
set -uo pipefail
cd "$(dirname "$0")/.."

MVN=(./mvnw -B -ntp -pl autto-e2e)
REPORTS=autto-e2e/target/autto-reports
FAILED=autto-e2e/target/rerun-failed.txt

"${MVN[@]}" test "$@"
first=$?
if [ "$first" -eq 0 ]; then
  exit 0
fi

if [ ! -s "$REPORTS/rerun.txt" ]; then
  echo "The build failed but no scenario failed (see the log above): not rerunning." >&2
  exit "$first"
fi

cp "$REPORTS/rerun.txt" "$FAILED"
rm -rf autto-e2e/target/autto-reports-first-run
cp -R "$REPORTS" autto-e2e/target/autto-reports-first-run
echo
echo "== Rerun pass: $(grep -c . "$FAILED") failed scenario(s) =="
cat "$FAILED"

"${MVN[@]}" test "$@" \
  -Dautto.rerun=true \
  -Dcucumber.features="$(paste -sd, "$FAILED")" \
  -Dautto.report.dir=target/autto-reports-rerun
second=$?

if [ "$second" -eq 0 ]; then
  echo "== All failed scenarios passed on the rerun: they are flaky, see autto-e2e/target/autto-reports-rerun =="
fi
exit "$second"
