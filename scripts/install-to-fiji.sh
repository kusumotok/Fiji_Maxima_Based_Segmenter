#!/usr/bin/env bash
# Optional helper to install built jar into local Fiji plugins directory.

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
FIJI_PLUGINS="${FIJI_PLUGINS:-$HOME/Fiji.app/plugins}"
JAR_FILE="$ROOT/plugin/target/Area_Segmentater.jar"

if [[ ! -f "$JAR_FILE" ]]; then
    echo "Error: jar not found: $JAR_FILE"
    echo "Run: mvn -f plugin/pom.xml package"
    exit 1
fi

if [[ ! -d "$FIJI_PLUGINS" ]]; then
    echo "Error: Fiji plugins directory not found: $FIJI_PLUGINS"
    echo "Set FIJI_PLUGINS to your Fiji plugins directory"
    exit 1
fi

echo "Copying $JAR_FILE -> $FIJI_PLUGINS/Area_Segmentater.jar"
cp "$JAR_FILE" "$FIJI_PLUGINS/Area_Segmentater.jar"
echo "Install complete. Restart Fiji to load the plugin."
