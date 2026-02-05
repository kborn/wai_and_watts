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
JAR="$REPO_ROOT/backend/target/backend-0.0.1-SNAPSHOT.jar"

if [[ ! -f "$JAR" ]]; then
  echo "ERROR: Backend jar not found: $JAR"
  echo "Build it with: mvn -f backend -DskipTests package"
  exit 2
fi

args=("$DATASET_SOURCE_CODE" "$FILE_PATH")
if [[ -n "$PUBLISHED_DATE" ]]; then
  args+=("$PUBLISHED_DATE")
fi
if [[ -n "$RELEASE_LABEL" ]]; then
  args+=("$RELEASE_LABEL")
fi

java -Dloader.main=nz.waiwatts.cli.ManualIngestionCommand -jar "$JAR" "${args[@]}"
exit $?
