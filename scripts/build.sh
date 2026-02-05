#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../plugin"
mvn -q package
echo "Built: plugin/target/Area_Segmentater.jar"
