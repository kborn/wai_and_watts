#!/usr/bin/env bash
set -euo pipefail

usage() {
  echo "Usage: ./scripts/transform.sh <dataset_source_code> <input_xlsx_path> <output_csv_path>"
  echo "Example: ./scripts/transform.sh mbie.generation.annual workbook.xlsx /tmp/mbie_annual.csv"
}

if [[ $# -ne 3 ]]; then
  usage
  exit 1
fi

DATASET_SOURCE_CODE="$1"
INPUT_PATH="$2"
OUTPUT_PATH="$3"

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

if [[ ! -f "$INPUT_PATH" ]]; then
  echo "ERROR: Input file does not exist: $INPUT_PATH"
  exit 2
fi

if [[ ! -r "$INPUT_PATH" ]]; then
  echo "ERROR: Input file is not readable: $INPUT_PATH"
  exit 2
fi

if [[ ! -s "$INPUT_PATH" ]]; then
  echo "ERROR: Input file is empty: $INPUT_PATH"
  exit 2
fi

OUT_DIR="$(dirname "$OUTPUT_PATH")"
mkdir -p "$OUT_DIR" 2>/dev/null || true
if [[ -d "$OUTPUT_PATH" ]]; then
  echo "ERROR: Output path is a directory: $OUTPUT_PATH"
  exit 2
fi
if [[ ! "$OUTPUT_PATH" =~ \.csv$ ]]; then
  echo "ERROR: Output path must end with .csv: $OUTPUT_PATH"
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
  echo "Build it with: mvn -f backend -DskipTests=true -Dmaven.test.skip=true package"
  exit 2
fi
echo "Using jar: $JAR"

args=("$DATASET_SOURCE_CODE" "$INPUT_PATH" "$OUTPUT_PATH")
java -cp "$JAR" \
  -Dloader.main=nz.waiwatts.cli.ManualTransformCommand \
  org.springframework.boot.loader.launch.PropertiesLauncher \
  "${args[@]}"
