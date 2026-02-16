#!/usr/bin/env bash
set -euo pipefail

# Wai & Watts Ingest Service
# Runs transform + ingest on bundled or mounted data files

PROVIDER="all"
BUNDLE_DATE=""
ALL_DATES=false
DB_URL="${DB_URL:-jdbc:postgresql://postgres:5432/waiwatts}"
DB_USER="${DB_USER:-waiwatts}"
DB_PASSWORD="${DB_PASSWORD:-waiwatts}"

APP_ROOT="/app"
DOWNLOADS_DIR="${APP_ROOT}/downloads"
PIPELINE_SCRIPT="${APP_ROOT}/scripts/pipeline.sh"

usage() {
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Options:"
    echo "  --provider PROVIDER   Provider to ingest: mbie, lawa, or all (default: all)"
    echo "  --bundle-date DATE    Specific bundle date to ingest (YYYY-MM-DD format)"
    echo "  --all-dates           Ingest all available dates"
    echo "  --db-url URL          Database URL (default: jdbc:postgresql://postgres:5432/waiwatts)"
    echo "  --help                Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0 --provider mbie"
    echo "  $0 --provider lawa --bundle-date 2026-02-06"
    echo "  $0 --all-dates"
}

while [[ $# -gt 0 ]]; do
    case $1 in
        --provider)
            PROVIDER="$2"
            shift 2
            ;;
        --bundle-date)
            BUNDLE_DATE="$2"
            shift 2
            ;;
        --date)
            echo "WARNING: --date is deprecated; use --bundle-date instead."
            BUNDLE_DATE="$2"
            shift 2
            ;;
        --all-dates)
            ALL_DATES=true
            shift
            ;;
        --db-url)
            DB_URL="$2"
            shift 2
            ;;
        --help)
            usage
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            usage
            exit 1
            ;;
    esac
done

if [[ ! -x "$PIPELINE_SCRIPT" ]]; then
    echo "ERROR: pipeline.sh not found or not executable at $PIPELINE_SCRIPT"
    exit 2
fi

# Export DB vars for pipeline -> ingest
export DB_URL DB_USER DB_PASSWORD

echo "=== Wai & Watts Ingest ==="
echo "Provider: $PROVIDER"
echo "Bundle Date: ${BUNDLE_DATE:-latest}"
echo ""

ingest_provider() {
    local provider=$1
    local date_dir=$2

    echo "Processing: $provider / $date_dir"

    if [[ "$provider" == "mbie" ]]; then
        "$PIPELINE_SCRIPT" \
            mbie.generation.annual \
            --bundle-date "$date_dir"

        "$PIPELINE_SCRIPT" \
            mbie.generation.quarterly \
            --bundle-date "$date_dir"

    elif [[ "$provider" == "lawa" ]]; then
        "$PIPELINE_SCRIPT" \
            lawa.water_quality.state.multi_year \
            --bundle-date "$date_dir"

        "$PIPELINE_SCRIPT" \
            lawa.water_quality.trend.multi_year \
            --bundle-date "$date_dir"
    fi
}

if [[ "$ALL_DATES" == "true" ]]; then
    for dir in "${DOWNLOADS_DIR}/mbie"/*/; do
        date_dir=$(basename "$dir")
        if [[ "$PROVIDER" == "all" ]] || [[ "$PROVIDER" == "mbie" ]]; then
            ingest_provider "mbie" "$date_dir"
        fi
    done
    for dir in "${DOWNLOADS_DIR}/lawa"/*/; do
        date_dir=$(basename "$dir")
        if [[ "$PROVIDER" == "all" ]] || [[ "$PROVIDER" == "lawa" ]]; then
            ingest_provider "lawa" "$date_dir"
        fi
    done
elif [[ -n "$BUNDLE_DATE" ]]; then
    if [[ "$PROVIDER" == "all" ]] || [[ "$PROVIDER" == "mbie" ]]; then
        if [[ -d "${DOWNLOADS_DIR}/mbie/$BUNDLE_DATE" ]]; then
            ingest_provider "mbie" "$BUNDLE_DATE"
        else
            echo "Warning: ${DOWNLOADS_DIR}/mbie/$BUNDLE_DATE not found"
        fi
    fi
    if [[ "$PROVIDER" == "all" ]] || [[ "$PROVIDER" == "lawa" ]]; then
        if [[ -d "${DOWNLOADS_DIR}/lawa/$BUNDLE_DATE" ]]; then
            ingest_provider "lawa" "$BUNDLE_DATE"
        else
            echo "Warning: ${DOWNLOADS_DIR}/lawa/$BUNDLE_DATE not found"
        fi
    fi
else
    latest_mbie=$(ls -1 "${DOWNLOADS_DIR}/mbie" 2>/dev/null | sort -r | head -1)
    latest_lawa=$(ls -1 "${DOWNLOADS_DIR}/lawa" 2>/dev/null | sort -r | head -1)

    if [[ "$PROVIDER" == "all" ]] || [[ "$PROVIDER" == "mbie" ]]; then
        if [[ -n "$latest_mbie" ]]; then
            ingest_provider "mbie" "$latest_mbie"
        else
            echo "Warning: No MBIE data found in ${DOWNLOADS_DIR}/mbie"
        fi
    fi
    if [[ "$PROVIDER" == "all" ]] || [[ "$PROVIDER" == "lawa" ]]; then
        if [[ -n "$latest_lawa" ]]; then
            ingest_provider "lawa" "$latest_lawa"
        else
            echo "Warning: No LAWA data found in ${DOWNLOADS_DIR}/lawa"
        fi
    fi
fi

echo ""
echo "=== Ingest Complete ==="
