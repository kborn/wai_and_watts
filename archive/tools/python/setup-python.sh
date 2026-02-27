#!/usr/bin/env bash
set -euo pipefail

# Optional bootstrap for archived Python scripts.
# Creates a local virtualenv in this directory and installs archived tooling deps.

if ! command -v python3 >/dev/null 2>&1; then
  echo "ERROR: python3 not found on PATH."
  echo "Install Python 3 (Mac: brew install python; Ubuntu: apt install python3 python3-venv)."
  exit 1
fi

python3 -m venv .venv
# shellcheck disable=SC1091
source .venv/bin/activate

python -m pip install --upgrade pip
pip install -r requirements.txt

echo ""
echo "Archived Python tooling environment created."
echo "Activate with: source .venv/bin/activate"
