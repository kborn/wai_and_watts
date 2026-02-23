#!/usr/bin/env python3
"""Phase 15 — Pattern Panel Evaluator.

Evaluates three JSONL runs against Gates A-C.
"""

from __future__ import annotations

import argparse
import json
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Dict, List, Optional


ALLOWED_REFUSAL_CODES = {
    "UNSUPPORTED_INTENT",
    "UNABLE_TO_PARSE",
    "UNSUPPORTED_CAPABILITY",
    "DATASET_MISMATCH",
    "MISSING_REQUIRED_FILTERS",
    "NO_DATA",
    "VALIDATION_FAILED",
    "LLM_REQUIRED",
}

SUPPORTED_ANSWER_RATE_MIN = 0.70
UNSUPPORTED_REFUSAL_RATE_MIN = 0.95


@dataclass(frozen=True)
class Outcome:
    outcome_class: str
    refusal_code: Optional[str]
    has_citations: bool
    is_supported_shape: bool


def load_run(path: Path) -> Dict[str, Outcome]:
    outcomes: Dict[str, Outcome] = {}
    with path.open("r", encoding="utf-8") as handle:
        for line in handle:
            if not line.strip():
                continue
            row = json.loads(line)
            meta = row.get("meta") or {}
            body = (row.get("result") or {}).get("body") or {}

            outcome_class = str(meta.get("outcome_class") or ("REFUSAL" if body.get("isRefusal") else "ANSWER"))
            refusal_code = meta.get("refusal_code")
            if refusal_code is None and outcome_class == "REFUSAL":
                refusal_code = ((body.get("refusal") or {}).get("code")) or "UNKNOWN"

            citations = body.get("citations") or []
            has_citations = True if outcome_class == "REFUSAL" else bool(citations)

            outcomes[str(meta.get("id"))] = Outcome(
                outcome_class=outcome_class,
                refusal_code=str(refusal_code) if refusal_code is not None else None,
                has_citations=has_citations,
                is_supported_shape=bool(meta.get("is_supported_shape")),
            )
    return outcomes


def evaluate_gate_a(runs: List[Dict[str, Outcome]]) -> List[str]:
    errors: List[str] = []
    for run_index, run in enumerate(runs, start=1):
        for case_id, outcome in run.items():
            if outcome.outcome_class == "ANSWER" and not outcome.has_citations:
                errors.append(f"run{run_index}:{case_id}: answer missing citations")
            if outcome.outcome_class == "REFUSAL" and outcome.refusal_code == "INTERNAL_ERROR":
                errors.append(f"run{run_index}:{case_id}: INTERNAL_ERROR refusal")
            if (
                outcome.outcome_class == "REFUSAL"
                and outcome.refusal_code
                and outcome.refusal_code not in ALLOWED_REFUSAL_CODES
                and outcome.refusal_code != "UNKNOWN"
            ):
                errors.append(f"run{run_index}:{case_id}: disallowed refusal code {outcome.refusal_code}")
    return errors


def evaluate_gate_b(run: Dict[str, Outcome]) -> Dict[str, Any]:
    supported = [o for o in run.values() if o.is_supported_shape]
    unsupported = [o for o in run.values() if not o.is_supported_shape]

    supported_answered = sum(1 for o in supported if o.outcome_class == "ANSWER")
    unsupported_refused = sum(1 for o in unsupported if o.outcome_class == "REFUSAL")

    supported_rate = (supported_answered / len(supported)) if supported else 0.0
    unsupported_rate = (unsupported_refused / len(unsupported)) if unsupported else 0.0

    errors: List[str] = []
    if supported_rate < SUPPORTED_ANSWER_RATE_MIN:
        errors.append(
            f"supported answer rate {supported_rate:.2%} below {SUPPORTED_ANSWER_RATE_MIN:.0%}"
        )
    if unsupported_rate < UNSUPPORTED_REFUSAL_RATE_MIN:
        errors.append(
            f"unsupported refusal rate {unsupported_rate:.2%} below {UNSUPPORTED_REFUSAL_RATE_MIN:.0%}"
        )

    return {
        "pass": not errors,
        "supported": {
            "total": len(supported),
            "answered": supported_answered,
            "rate": supported_rate,
        },
        "unsupported": {
            "total": len(unsupported),
            "refused": unsupported_refused,
            "rate": unsupported_rate,
        },
        "errors": errors,
    }


def evaluate_gate_c(runs: List[Dict[str, Outcome]]) -> List[str]:
    errors: List[str] = []
    shared_ids = set(runs[0].keys())
    for run in runs[1:]:
        shared_ids &= set(run.keys())

    for case_id in sorted(shared_ids):
        baseline = runs[0][case_id]
        for index in range(1, len(runs)):
            candidate = runs[index][case_id]
            if baseline.outcome_class != candidate.outcome_class:
                errors.append(
                    f"{case_id}: outcome differs ({baseline.outcome_class} vs {candidate.outcome_class})"
                )
            if (
                baseline.outcome_class == "REFUSAL"
                and baseline.refusal_code != candidate.refusal_code
            ):
                errors.append(
                    f"{case_id}: refusal code differs ({baseline.refusal_code} vs {candidate.refusal_code})"
                )
    return errors


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--run", action="append", required=True, help="Path to JSONL run output")
    args = parser.parse_args()

    if len(args.run) != 3:
        print("Provide exactly 3 --run files", file=sys.stderr)
        sys.exit(1)

    run_paths = [Path(p) for p in args.run]
    runs = [load_run(path) for path in run_paths]

    gate_a_errors = evaluate_gate_a(runs)
    gate_b = evaluate_gate_b(runs[0])
    gate_c_errors = evaluate_gate_c(runs)

    report = {
        "gateA_safety": {
            "pass": not gate_a_errors,
            "errors": gate_a_errors[:100],
        },
        "gateB_reliability": gate_b,
        "gateC_determinism": {
            "pass": not gate_c_errors,
            "errors": gate_c_errors[:100],
        },
    }

    print(json.dumps(report, indent=2))
    if gate_a_errors or gate_b["errors"] or gate_c_errors:
        sys.exit(1)


if __name__ == "__main__":
    main()
