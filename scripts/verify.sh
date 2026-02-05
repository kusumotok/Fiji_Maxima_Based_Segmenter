#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
echo "[verify] repo: $ROOT"

echo "[verify] build plugin..."
bash "$ROOT/scripts/build.sh"

JAR="$ROOT/plugin/target/Area_Segmentater.jar"
if [[ ! -f "$JAR" ]]; then
  echo "[verify] FAIL: jar not found: $JAR"
  exit 1
fi

echo "[verify] PASS: jar exists: $JAR"
