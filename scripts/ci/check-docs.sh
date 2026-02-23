#!/usr/bin/env bash
set -euo pipefail

echo "[docs-check] Running canonical docs checks..."

CANONICAL_PATHS=(
  "README.md"
  "docs"
  "engineering"
)

FORBIDDEN_PATTERNS=(
  "docs/ai-dev/"
  "docs/phase-notes/"
  "docs/operations/"
  "docs/validation/"
  "docs/archive/phase15/2026-02-doc-convergence/"
  "docs/README.md"
  "docs/REPO_MAP.md"
  "docs/PORTFOLIO_ENGINEERING_PROCESS.md"
)

echo "[docs-check] Path lint (legacy references)..."
for pattern in "${FORBIDDEN_PATTERNS[@]}"; do
  if rg -n --glob '*.md' --glob '!archive/**' --fixed-strings "$pattern" "${CANONICAL_PATHS[@]}" >/tmp/docs_check_hits.txt; then
    echo "[docs-check] ERROR: found forbidden canonical reference pattern: $pattern"
    cat /tmp/docs_check_hits.txt
    exit 1
  fi
done

echo "[docs-check] Local markdown link validation..."
fail=0
while IFS= read -r file; do
  file_dir="$(dirname "$file")"

  while IFS= read -r link; do
    link="${link%% *}"          # Drop optional title text.
    link="${link#<}"            # Trim optional angle-bracket wrapping.
    link="${link%>}"

    if [[ -z "$link" ]]; then
      continue
    fi
    if [[ "$link" =~ ^https?:// ]] || [[ "$link" =~ ^mailto: ]] || [[ "$link" =~ ^# ]]; then
      continue
    fi

    path="${link%%#*}"          # Drop in-file anchor.
    if [[ -z "$path" ]]; then
      continue
    fi

    if [[ "$path" == /* ]]; then
      target=".${path}"
    else
      target="${file_dir}/${path}"
    fi

    if [[ ! -e "$target" ]]; then
      echo "[docs-check] ERROR: broken local link in $file -> $link"
      fail=1
    fi
  done < <(perl -ne 'while(/\[[^\]]+\]\(([^)]+)\)/g){print "$1\n"}' "$file")
done < <(find README.md docs engineering -type f -name '*.md' | sort)

if [[ "$fail" -ne 0 ]]; then
  exit 1
fi

echo "[docs-check] All checks passed."
