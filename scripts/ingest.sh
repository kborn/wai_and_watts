#!/usr/bin/env bash
set -euo pipefail

usage() {
  echo "Usage: ./scripts/ingest.sh <dataset_source_code> <file_path> [published_date] [release_label]"
  echo "Example: ./scripts/ingest.sh mbie.generation.annual data.csv 2025-01-01 \"MBIE Workbook\""
}

if [[ $# -lt 2 || $# -gt 4 ]]; then
  usage
  exit 1
fi

DATASET_SOURCE_CODE="$1"
FILE_PATH="$2"
PUBLISHED_DATE="${3:-}"
RELEASE_LABEL="${4:-}"

KNOWN_DATASETS=(
  "mbie.generation.annual"
  "mbie.generation.quarterly"
  "lawa.water_quality.state.multi_year"
  "lawa.water_quality.trend.multi_year"
)

dataset_known=false
for code in "${KNOWN_DATASETS[@]}"; do
  if [[ "$code" == "$DATASET_SOURCE_CODE" ]]; then
    dataset_known=true
    break
  fi
done

if [[ "$dataset_known" != "true" ]]; then
  echo "ERROR: Unknown dataset source code: $DATASET_SOURCE_CODE"
  exit 2
fi

if [[ ! -f "$FILE_PATH" ]]; then
  echo "ERROR: File does not exist: $FILE_PATH"
  exit 2
fi

if [[ ! -r "$FILE_PATH" ]]; then
  echo "ERROR: File is not readable: $FILE_PATH"
  exit 2
fi

if [[ ! -s "$FILE_PATH" ]]; then
  echo "ERROR: File is empty: $FILE_PATH"
  exit 2
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
shopt -s nullglob
JAR_CANDIDATES=("$REPO_ROOT"/backend/target/*.jar)
JAR=""
for candidate in "${JAR_CANDIDATES[@]}"; do
  if [[ "$candidate" == *original* ]]; then
    continue
  fi
  if [[ -z "$JAR" || "$candidate" -nt "$JAR" ]]; then
    JAR="$candidate"
  fi
done

if [[ -z "$JAR" || ! -f "$JAR" ]]; then
  echo "ERROR: Backend jar not found in: $REPO_ROOT/backend/target"
  echo "Build it with: mvn -f backend clean package spring-boot:repackage -DskipTests"
  exit 2
fi
echo "Using jar: $JAR"

args=("$DATASET_SOURCE_CODE" "$FILE_PATH")
if [[ -n "$PUBLISHED_DATE" ]]; then
  args+=("$PUBLISHED_DATE")
fi
if [[ -n "$RELEASE_LABEL" ]]; then
  args+=("$RELEASE_LABEL")
fi

java -cp "$JAR" \
  -Dloader.main=nz.waiwatts.cli.ManualIngestionCommand \
  org.springframework.boot.loader.launch.PropertiesLauncher \
  "${args[@]}"
