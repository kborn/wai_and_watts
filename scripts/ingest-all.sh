#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

usage() {
  echo "Wai & Watts - Ingest All (Manifest)"
  echo ""
  echo "Usage:"
  echo "  $0 --bundle-date YYYY-MM-DD"
  echo "  $0 --help"
  echo ""
  echo "Options:"
  echo "  --bundle-date DATE   Bundle date to ingest (YYYY-MM-DD)"
  echo ""
  echo "Notes:"
  echo "  - Reads downloads/manifest/<bundle-date>.json"
  echo "  - For each dataset entry, runs pipeline.sh with published date and release label"
}

if [[ "${1:-}" == "--help" || "${1:-}" == "-h" ]]; then
  usage
  exit 0
fi

BUNDLE_DATE=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --bundle-date)
      BUNDLE_DATE="${2:-}"
      shift 2
      ;;
    *)
      echo "ERROR: Unknown option: $1"
      usage
      exit 1
      ;;
  esac
done

if [[ -z "$BUNDLE_DATE" ]]; then
  echo "ERROR: --bundle-date is required"
  usage
  exit 1
fi

MANIFEST_PATH="${REPO_ROOT}/downloads/manifest/${BUNDLE_DATE}.json"
if [[ ! -f "$MANIFEST_PATH" ]]; then
  echo "ERROR: Manifest not found: $MANIFEST_PATH"
  exit 1
fi

if ! command -v jq >/dev/null 2>&1; then
  echo "ERROR: jq is required to parse the manifest"
  exit 1
fi

mapfile -t datasets < <(jq -r 'keys[]' "$MANIFEST_PATH")
if [[ ${#datasets[@]} -eq 0 ]]; then
  echo "ERROR: Manifest is empty: $MANIFEST_PATH"
  exit 1
fi

for dataset in "${datasets[@]}"; do
  published_date=$(jq -r --arg key "$dataset" '.[$key].published_date // empty' "$MANIFEST_PATH")
  release_label=$(jq -r --arg key "$dataset" '.[$key].release_label // empty' "$MANIFEST_PATH")

  args=("$dataset" "--bundle-date" "$BUNDLE_DATE")
  if [[ -n "$published_date" ]]; then
    args+=("--published-date" "$published_date")
  fi
  if [[ -n "$release_label" ]]; then
    args+=("--release-label" "$release_label")
  fi

  "$SCRIPT_DIR/pipeline.sh" "${args[@]}"
done
