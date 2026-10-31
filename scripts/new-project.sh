#!/usr/bin/env bash
# Creates a new, standalone test project on top of autto-core.
#
#   ./scripts/new-project.sh <target-dir> <groupId> <artifactId> [--local]
#   ./scripts/new-project.sh ../checkout-e2e com.acme checkout-e2e
#
# By default the project resolves autto-core from JitPack (zero setup). With --local it uses the copy installed in
# your local Maven repository (./mvnw install), useful to try unreleased changes or to work offline.
set -euo pipefail

if [ "$#" -lt 3 ]; then
  sed -n '2,8p' "$0" | sed 's/^# \{0,1\}//'
  exit 1
fi

root="$(cd "$(dirname "$0")/.." && pwd)"
target="$1"; group="$2"; artifact="$3"; mode="${4:-jitpack}"

if [ -e "$target" ] && [ -n "$(ls -A "$target" 2>/dev/null)" ]; then
  echo "$target already exists and is not empty" >&2
  exit 1
fi

version="$(cd "$root" && ./mvnw -q help:evaluate -Dexpression=project.version -DforceStdout)"
package="$(echo "$group.$artifact" | tr 'A-Z' 'a-z' | sed 's/[^a-z0-9.]//g')"
package_path="$(echo "$package" | tr . /)"

if [ "$mode" = "--local" ]; then
  autto_group="io.github.andercmd"
  autto_version="$version"
  repositories=""
else
  autto_group="com.github.AnderCMD.Autto-Framework"
  autto_version="v$version"
  repositories='<repositories>
        <repository>
            <id>jitpack.io</id>
            <url>https://jitpack.io</url>
        </repository>
    </repositories>'
fi

mkdir -p "$target"
cp -R "$root/templates/starter/." "$target/"

# rename the package folder and substitute the placeholders
mkdir -p "$target/src/test/java/$package_path"
cp -R "$target/src/test/java/__PACKAGE_PATH__/." "$target/src/test/java/$package_path/"
rm -rf "$target/src/test/java/__PACKAGE_PATH__"

while IFS= read -r -d '' file; do
  PACKAGE="$package" GROUP="$group" ARTIFACT="$artifact" AUTTO_GROUP="$autto_group" \
  AUTTO_VERSION="$autto_version" REPOSITORIES="$repositories" python3 - "$file" <<'PY'
import os, sys
path = sys.argv[1]
text = open(path, encoding="utf-8").read()
for key, env in (("__PACKAGE__", "PACKAGE"), ("__GROUP__", "GROUP"), ("__ARTIFACT__", "ARTIFACT"),
                 ("__AUTTO_GROUP__", "AUTTO_GROUP"), ("__AUTTO_VERSION__", "AUTTO_VERSION"),
                 ("__REPOSITORIES__", "REPOSITORIES")):
    text = text.replace(key, os.environ[env])
open(path, "w", encoding="utf-8").write(text)
PY
done < <(find "$target" -type f -print0)

# Maven wrapper from this repository, so the project builds without a Maven installation
cp "$root/mvnw" "$root/mvnw.cmd" "$target/"
mkdir -p "$target/.mvn/wrapper"
cp "$root/.mvn/wrapper/maven-wrapper.properties" "$target/.mvn/wrapper/"

echo "Created $target ($package) using autto-core $autto_group:$autto_version"
echo "Next:  cd $target && cp .env.example .env && ./mvnw test"
