#!/usr/bin/env bash
set -euo pipefail

rm -f out_run1.jsonl out_run2.jsonl out_run3.jsonl

python3 docs/phase-notes/phase15/pattern_panel_runner.py --base-url http://localhost:8080 --out out_run1.jsonl --seed 42
python3 docs/phase-notes/phase15/pattern_panel_runner.py --base-url http://localhost:8080 --out out_run2.jsonl --seed 42
python3 docs/phase-notes/phase15/pattern_panel_runner.py --base-url http://localhost:8080 --out out_run3.jsonl --seed 42

python3 docs/phase-notes/phase15/pattern_panel_evaluate.py --run out_run1.jsonl --run out_run2.jsonl --run out_run3.jsonl
