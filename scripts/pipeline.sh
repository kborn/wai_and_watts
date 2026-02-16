#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

usage() {
  echo "Wai & Watts - Full Ingestion Pipeline"
  echo ""
  echo "Transforms and ingests data in one step."
  echo ""
  echo "Usage:"
  echo "  $0 <dataset_source_code> --input /path/to/workbook.xlsx [--published-date YYYY-MM-DD] [--release-label \"Label\"]"
  echo "  $0 <dataset_source_code> --bundle-date YYYY-MM-DD [--published-date YYYY-MM-DD] [--release-label \"Label\"]"
  echo "  $0 --help"
  echo ""
  echo "Arguments:"
  echo "  dataset_source_code    Dataset identifier (e.g., mbie.generation.annual)"
  echo ""
  echo "Options:"
  echo "  --bundle-date DATE     Use downloads/<provider>/<date>/... workbook path"
  echo "  --input PATH           Explicit workbook path override"
  echo "  --published-date DATE  Publication date (YYYY-MM-DD)"
  echo "  --release-label LABEL  Release label"
  echo ""
  echo "Examples:"
  echo "  # Transform and ingest"
  echo "  $0 mbie.generation.annual --input ./downloads/mbie/workbook.xlsx"
  echo ""
  echo "  # With published date and label"
  echo "  $0 mbie.generation.annual --input ./downloads/mbie/workbook.xlsx --published-date 2025-01-01 --release-label \"Q3 2025\""
  echo ""
  echo "  # Resolve input path from downloads/ by date"
  echo "  $0 mbie.generation.annual --bundle-date 2026-02-06 --published-date 2025-01-01 --release-label \"Q3 2025\""
  echo ""
  echo "Environment:"
  echo "  DB_URL       Database connection URL (default: jdbc:postgresql://localhost:5432/waiwatts)"
  echo "  DB_USER      Database user (default: waiwatts)"
  echo "  DB_PASSWORD  Database password (default: waiwatts)"
}

if [[ "${1:-}" == "--help" || "${1:-}" == "-h" ]]; then
  usage
  exit 0
fi

if [[ $# -lt 2 ]]; then
  usage
  exit 1
fi

DATASET_SOURCE_CODE="$1"
shift

INPUT_XLSX=""
DATE_HINT=""
PUBLISHED_DATE=""
RELEASE_LABEL=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --bundle-date)
      DATE_HINT="${2:-}"
      shift 2
      ;;
    --input)
      INPUT_XLSX="${2:-}"
      shift 2
      ;;
    --published-date)
      PUBLISHED_DATE="${2:-}"
      shift 2
      ;;
    --release-label)
      RELEASE_LABEL="${2:-}"
      shift 2
      ;;
    --date)
      echo "WARNING: --date is deprecated; use --bundle-date instead."
      DATE_HINT="${2:-}"
      shift 2
      ;;
    *)
      echo "ERROR: Unknown option: $1"
      usage
      exit 1
      ;;
  esac
done

if [[ -n "$INPUT_XLSX" && -n "$DATE_HINT" ]]; then
  echo "ERROR: Use either --input or --bundle-date, not both."
  exit 1
fi

DB_URL="${DB_URL:-jdbc:postgresql://localhost:5432/waiwatts}"
DB_USER="${DB_USER:-waiwatts}"
DB_PASSWORD="${DB_PASSWORD:-waiwatts}"

if [[ -z "$INPUT_XLSX" && -n "$DATE_HINT" ]]; then
  case "$DATASET_SOURCE_CODE" in
    mbie.generation.annual|mbie.generation.quarterly)
      INPUT_XLSX="${REPO_ROOT}/downloads/mbie/${DATE_HINT}/electricity-sept-2025-q3.xlsx"
      ;;
    lawa.water_quality.state.multi_year|lawa.water_quality.trend.multi_year)
      INPUT_XLSX="${REPO_ROOT}/downloads/lawa/${DATE_HINT}/lawa-river-water-quality-state-and-trend-results_30oct2025.xlsx"
      ;;
    *)
      echo "ERROR: Unknown dataset source code: $DATASET_SOURCE_CODE"
      exit 1
      ;;
  esac
fi

if [[ -z "$INPUT_XLSX" ]]; then
  echo "ERROR: Input file not specified."
  usage
  exit 1
fi

if [[ ! -f "$INPUT_XLSX" ]]; then
  echo "ERROR: Input file not found: $INPUT_XLSX"
  exit 1
fi

if [[ -n "$DATE_HINT" ]]; then
  MANIFEST_PATH="${REPO_ROOT}/downloads/manifest/${DATE_HINT}.json"
  if [[ -f "$MANIFEST_PATH" ]]; then
    if [[ -z "$PUBLISHED_DATE" ]]; then
      PUBLISHED_DATE=$(jq -r --arg key "$DATASET_SOURCE_CODE" '.[$key].published_date // empty' "$MANIFEST_PATH")
    fi
    if [[ -z "$RELEASE_LABEL" ]]; then
      RELEASE_LABEL=$(jq -r --arg key "$DATASET_SOURCE_CODE" '.[$key].release_label // empty' "$MANIFEST_PATH")
    fi
  fi
fi

TIMESTAMP=$(date +%Y-%m-%d)

shopt -s nullglob

case "$DATASET_SOURCE_CODE" in
  mbie.generation.annual)
    OUTPUT_FILE="mbie_generation_annual.csv"
    ;;
  mbie.generation.quarterly)
    OUTPUT_FILE="mbie_generation_quarterly.csv"
    ;;
  lawa.water_quality.state.multi_year)
    OUTPUT_FILE="lawa_state_multi_year.csv"
    ;;
  lawa.water_quality.trend.multi_year)
    OUTPUT_FILE="lawa_trend_multi_year.csv"
    ;;
  *)
    OUTPUT_FILE="${DATASET_SOURCE_CODE//./_}.csv"
    ;;
esac

TRANSFORMED_CSV="${REPO_ROOT}/transforms/${DATASET_SOURCE_CODE}/${TIMESTAMP}/${OUTPUT_FILE}"

echo "=== Step 1: Transform ==="
echo "Input:  $INPUT_XLSX"
echo "Output: $TRANSFORMED_CSV"
"$SCRIPT_DIR/transform.sh" "$DATASET_SOURCE_CODE" "$INPUT_XLSX"

echo ""
echo "=== Step 2: Ingest ==="
export DB_URL DB_USER DB_PASSWORD

"$SCRIPT_DIR/ingest.sh" "$DATASET_SOURCE_CODE" "$TRANSFORMED_CSV" "$PUBLISHED_DATE" "$RELEASE_LABEL"

echo ""
echo "=== Done ==="
echo "Dataset: $DATASET_SOURCE_CODE"
