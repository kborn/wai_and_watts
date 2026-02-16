#!/usr/bin/env bash
set -euo pipefail

usage() {
  echo "Wai & Watts Transform Script"
  echo ""
  echo "Transforms publisher XLSX workbooks to canonical contract CSV files"
  echo ""
  echo "Usage:"
  echo "  $0 <dataset_source_code> <input_xlsx_path> [OPTIONS]"
  echo "  $0 --help"
  echo ""
  echo "Arguments:"
  echo "  dataset_source_code    Dataset identifier (e.g., mbie.generation.annual)"
  echo "  input_xlsx_path        Path to the Excel workbook to transform"
  echo ""
  echo "Options:"
  echo "  --output-dir DIR       Specify output directory (default: ./transforms/<dataset_code>/)"
  echo "  --output-file FILE     Specify output filename (default: auto-generated)"
  echo "  --help                 Show this help message"
  echo ""
  echo "Examples:"
  echo "  $0 mbie.generation.annual ./downloads/mbie/2026-02-06/workbook.xlsx"
  echo "  $0 mbie.generation.annual ./downloads/mbie/2026-02-06/workbook.xlsx --output-dir /tmp"
  echo "  $0 lawa.water_quality.state.multi_year ./downloads/lawa/2026-02-06/workbook.xlsx"
  echo ""
  echo "The script will:"
  echo "  1. Create ./transforms/<dataset_code>/<YYYY-MM-DD>/ directory"
  echo "  2. Transform XLSX to canonical CSV contract"
  echo "  3. Save with auto-generated filename based on dataset code"
}

# Default configuration
TIMESTAMP=$(date +%Y-%m-%d)
CUSTOM_OUTPUT_DIR=""
CUSTOM_OUTPUT_FILE=""

# Parse arguments
ARGS=()
while [[ $# -gt 0 ]]; do
  case $1 in
    --help|-h)
      usage
      exit 0
      ;;
    --output-dir)
      CUSTOM_OUTPUT_DIR="$2"
      shift 2
      ;;
    --output-file)
      CUSTOM_OUTPUT_FILE="$2"
      shift 2
      ;;
    -*)
      echo "ERROR: Unknown option: $1"
      usage
      exit 1
      ;;
    *)
      ARGS+=("$1")
      shift
      ;;
  esac
done

# Check if we have the right number of positional arguments
if [[ ${#ARGS[@]} -ne 2 ]]; then
  echo "ERROR: Missing required arguments"
  usage
  exit 1
fi

DATASET_SOURCE_CODE="${ARGS[0]}"
INPUT_PATH="${ARGS[1]}"

# Set up output directory and filename
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

if [[ -n "$CUSTOM_OUTPUT_DIR" ]]; then
  OUTPUT_DIR="$CUSTOM_OUTPUT_DIR"
else
  OUTPUT_DIR="${REPO_ROOT}/transforms/${DATASET_SOURCE_CODE}/${TIMESTAMP}"
fi

# Generate default filename based on dataset code
if [[ -z "$CUSTOM_OUTPUT_FILE" ]]; then
  case "$DATASET_SOURCE_CODE" in
    "mbie.generation.annual")
      OUTPUT_FILE="mbie_generation_annual.csv"
      ;;
    "mbie.generation.quarterly")
      OUTPUT_FILE="mbie_generation_quarterly.csv"
      ;;
    "lawa.water_quality.state.multi_year")
      OUTPUT_FILE="lawa_state_multi_year.csv"
      ;;
    "lawa.water_quality.trend.multi_year")
      OUTPUT_FILE="lawa_trend_multi_year.csv"
      ;;
    *)
      # Fallback: replace dots with underscores and add .csv
      OUTPUT_FILE="${DATASET_SOURCE_CODE//./_}.csv"
      ;;
  esac
else
  OUTPUT_FILE="$CUSTOM_OUTPUT_FILE"
fi

OUTPUT_PATH="${OUTPUT_DIR}/${OUTPUT_FILE}"

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

# Validate output file
if [[ -d "$OUTPUT_PATH" ]]; then
  echo "ERROR: Output path is a directory: $OUTPUT_PATH"
  exit 2
fi
if [[ ! "$OUTPUT_PATH" =~ \.csv$ ]]; then
  echo "ERROR: Output path must end with .csv: $OUTPUT_PATH"
  exit 2
fi

# Create output directory
mkdir -p "$(dirname "$OUTPUT_PATH")"
echo "Transform directory: $(dirname "$OUTPUT_PATH")"
echo "Output file: $OUTPUT_FILE"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
JAR="${WAIWATTS_JAR:-}"

if [[ -z "$JAR" ]]; then
  shopt -s nullglob
  JAR_CANDIDATES=("$REPO_ROOT"/backend/target/*.jar)
  if [[ ${#JAR_CANDIDATES[@]} -gt 0 ]]; then
    for candidate in "${JAR_CANDIDATES[@]}"; do
      if [[ "$candidate" == *original* ]]; then
        continue
      fi
      if [[ -z "$JAR" || "$candidate" -nt "$JAR" ]]; then
        JAR="$candidate"
      fi
    done
  fi
fi

if [[ -z "$JAR" && -f "/app/app.jar" ]]; then
  JAR="/app/app.jar"
fi

if [[ -z "$JAR" || ! -f "$JAR" ]]; then
  echo "ERROR: Backend jar not found."
  echo "Build it with: mvn -f backend clean package spring-boot:repackage -DskipTests"
  echo "Or set WAIWATTS_JAR to a valid jar path."
  exit 2
fi
echo "Using jar: $JAR"

args=("$DATASET_SOURCE_CODE" "$INPUT_PATH" "$OUTPUT_PATH")
java -cp "$JAR" \
  -Dloader.main=nz.waiwatts.cli.ManualTransformCommand \
  org.springframework.boot.loader.launch.PropertiesLauncher \
  "${args[@]}"
