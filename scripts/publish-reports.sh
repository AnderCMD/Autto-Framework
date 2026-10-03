#!/usr/bin/env bash
# Adds the reports of one CI run to a GitHub Pages checkout and rebuilds the history page.
#
#   ./scripts/publish-reports.sh <downloaded-artifacts-dir> <pages-dir> <run-number> <run-url>
#
# Layout:  <pages>/runs/<run-number>/<artifact-name>/...   (every report folder of the run)
#          <pages>/index.html                              (history, newest first)
set -euo pipefail

reports="$1"; pages="$2"; run="$3"; url="$4"

mkdir -p "$pages/runs/$run"
for dir in "$reports"/*/; do
  [ -d "$dir" ] || continue
  name="$(basename "$dir")"
  rm -rf "$pages/runs/$run/$name"
  cp -R "$dir" "$pages/runs/$run/$name"
done
touch "$pages/.nojekyll"
echo "$url" > "$pages/runs/$run/run-url.txt"

{
  cat <<'HTML'
<!doctype html>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Autto · Nightly reports</title>
<style>
  :root { color-scheme: light dark; font-family: system-ui, sans-serif; }
  body { max-width: 52rem; margin: 2rem auto; padding: 0 1rem; }
  h1 { font-size: 1.4rem; }
  details { border: 1px solid #8884; border-radius: .5rem; padding: .6rem 1rem; margin: .6rem 0; }
  summary { cursor: pointer; font-weight: 600; }
  ul { margin: .5rem 0 0; }
</style>
<h1>Autto · Nightly reports</h1>
HTML
  for run_dir in $(ls -1 "$pages/runs" | sort -rn); do
    echo "<details$([ "$run_dir" = "$run" ] && echo ' open')><summary>Run #$run_dir</summary><ul>"
    if [ -f "$pages/runs/$run_dir/run-url.txt" ]; then
      echo "<li><a href=\"$(cat "$pages/runs/$run_dir/run-url.txt")\">CI run</a></li>"
    fi
    for report in "$pages/runs/$run_dir"/*/; do
      [ -d "$report" ] || continue
      rname="$(basename "$report")"
      for index in "$report"*/index.html; do
        [ -f "$index" ] || continue
        rel="runs/$run_dir/$rname/$(basename "$(dirname "$index")")/index.html"
        echo "<li><a href=\"$rel\">$rname · $(basename "$(dirname "$index")")</a></li>"
      done
    done
    echo "</ul></details>"
  done
} > "$pages/index.html"
