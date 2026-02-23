# Phase 15 Generated Pattern Panel Harness

This harness validates Phase 15 Ask behavior using generated prompt shapes instead of a static question list.

## Files
- `docs/phase-notes/phase15/pattern_panel_runner.py`
- `docs/phase-notes/phase15/pattern_panel_evaluate.py`

## What the runner does
- Generates seedable supported + unsupported prompt sets
- Calls `POST /api/v1/explanations/ask`
- Writes JSONL rows with prompt metadata, response body, outcome class, and refusal code
- Emits a run summary (rates, refusal distribution, safety signals)

## What the evaluator checks
- Gate A: no uncited answers, no `INTERNAL_ERROR`
- Gate B: supported answer rate >= 70%, unsupported refusal rate >= 95%
- Gate C: determinism across 3 runs (same outcome class and refusal code per prompt ID)

## Run (3 passes)
```bash
rm -f out_run1.jsonl out_run2.jsonl out_run3.jsonl

python3 docs/phase-notes/phase15/pattern_panel_runner.py --base-url http://localhost:8080 --out out_run1.jsonl --seed 42
python3 docs/phase-notes/phase15/pattern_panel_runner.py --base-url http://localhost:8080 --out out_run2.jsonl --seed 42
python3 docs/phase-notes/phase15/pattern_panel_runner.py --base-url http://localhost:8080 --out out_run3.jsonl --seed 42

python3 docs/phase-notes/phase15/pattern_panel_evaluate.py --run out_run1.jsonl --run out_run2.jsonl --run out_run3.jsonl
```

## Notes
- Use identical `--seed` for reproducibility across runs.
- Restart backend between runs when checking Gate C.
