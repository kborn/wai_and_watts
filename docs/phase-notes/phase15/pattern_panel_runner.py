#!/usr/bin/env python3
"""Phase 15 — Generated Pattern Panel Runner.

Generates seedable supported/unsupported prompt panels, calls /api/v1/explanations/ask,
and writes one JSON object per prompt to JSONL.
"""

from __future__ import annotations

import argparse
import dataclasses
import json
import random
import sys
import time
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple

import requests


SUPPORTED_TEMPLATES = {
    "TREND": [
        "Explain {fuel} generation trends between {startYear} and {endYear}.",
        "How has {fuel} generation changed since {startYear}?",
        "Describe renewable generation trends between {startYear} and {endYear}.",
    ],
    "OVERVIEW": [
        "What are the main sources of electricity generation in New Zealand?",
        "What does the generation mix look like in {year}?",
        "Summarize electricity generation mix today.",
    ],
    "COMPARE": [
        "Compare {fuelA} and {fuelB} generation patterns.",
        "Compare {fuelA} vs {fuelB} trends over the last {nYears} years.",
    ],
    "LAWA": [
        "What does river water quality look like across New Zealand right now?",
        "Which regions have the highest proportion of poor river water quality sites?",
        "Are more river sites improving or degrading in water quality over time?",
        "How do water quality trends compare across regions?",
    ],
}

UNSUPPORTED_TEMPLATES = {
    "CAUSATION": [
        "Why did {fuel} generation drop in {year}?",
        "What caused river quality to decline in {region}?",
    ],
    "FORECAST": [
        "Will {fuelA} overtake {fuelB} by {year}?",
    ],
    "POLICY": [
        "Should NZ invest more in {fuel}?",
    ],
    "DERIVED_ANALYTICS": [
        "Which fuel has grown the most since {startYear}?",
        "In what {window}-year period did NZ have the biggest increase in {fuel} generation?",
        "When did renewables first exceed {threshold}% of total generation?",
        "When did {fuel} generation grow the fastest over any {window}-year period?",
    ],
}

FUELS = ["hydro", "wind", "solar", "geothermal", "coal", "gas"]
REGIONS = ["Auckland", "Waikato", "Canterbury", "Otago", "Bay of Plenty"]
YEARS = [2000, 2005, 2010, 2018, 2020, 2023, 2024]
WINDOWS = [3, 5, 10]
THRESHOLDS = [80]


@dataclasses.dataclass(frozen=True)
class PromptCase:
    case_id: str
    shape_group: str
    is_supported_shape: bool
    question: str


def _choose(rng: random.Random, values: List[Any]) -> Any:
    return values[rng.randrange(0, len(values))]


def _render_template(rng: random.Random, template: str) -> str:
    start_year = _choose(rng, [y for y in YEARS if y <= 2020])
    end_year = _choose(rng, [y for y in YEARS if y >= 2023])
    if end_year <= start_year:
        end_year = start_year + 1

    fuel_a = _choose(rng, FUELS)
    fuel_b = _choose(rng, [f for f in FUELS if f != fuel_a])

    context = {
        "fuel": _choose(rng, FUELS),
        "fuelA": fuel_a,
        "fuelB": fuel_b,
        "region": _choose(rng, REGIONS),
        "year": 2024,
        "startYear": start_year,
        "endYear": end_year,
        "nYears": _choose(rng, [10, 20]),
        "window": _choose(rng, WINDOWS),
        "threshold": _choose(rng, THRESHOLDS),
    }
    return template.format(**context)


def generate_panel(seed: int, supported_n: int, unsupported_n: int) -> List[PromptCase]:
    rng = random.Random(seed)
    cases: List[PromptCase] = []

    supported_index = 0
    for group in ["TREND", "OVERVIEW", "COMPARE", "LAWA"]:
        for _ in range(min(10, supported_n // 4)):
            template = _choose(rng, SUPPORTED_TEMPLATES[group])
            cases.append(
                PromptCase(
                    case_id=f"S-{group}-{supported_index:03d}",
                    shape_group=group,
                    is_supported_shape=True,
                    question=_render_template(rng, template),
                )
            )
            supported_index += 1

    while sum(1 for c in cases if c.is_supported_shape) < supported_n:
        group = _choose(rng, list(SUPPORTED_TEMPLATES.keys()))
        template = _choose(rng, SUPPORTED_TEMPLATES[group])
        cases.append(
            PromptCase(
                case_id=f"S-{group}-{supported_index:03d}",
                shape_group=group,
                is_supported_shape=True,
                question=_render_template(rng, template),
            )
        )
        supported_index += 1

    unsupported_index = 0
    while sum(1 for c in cases if not c.is_supported_shape) < unsupported_n:
        group = _choose(rng, list(UNSUPPORTED_TEMPLATES.keys()))
        template = _choose(rng, UNSUPPORTED_TEMPLATES[group])
        cases.append(
            PromptCase(
                case_id=f"U-{group}-{unsupported_index:03d}",
                shape_group=group,
                is_supported_shape=False,
                question=_render_template(rng, template),
            )
        )
        unsupported_index += 1

    rng.shuffle(cases)
    return cases


def call_ask(base_url: str, question: str, timeout_s: int) -> Dict[str, Any]:
    url = base_url.rstrip("/") + "/api/v1/explanations/ask"
    try:
        response = requests.post(url, json={"question": question}, timeout=timeout_s)
        try:
            payload = response.json()
        except Exception:
            payload = {"_non_json_body": response.text}
        return {"http": {"status": response.status_code}, "body": payload}
    except Exception as exc:
        return {
            "http": {"status": 0},
            "body": {
                "isRefusal": True,
                "refusal": {
                    "code": "INTERNAL_ERROR",
                    "message": f"Runner transport error: {type(exc).__name__}",
                },
            },
        }


def classify_outcome(body: Dict[str, Any]) -> Tuple[str, Optional[str]]:
    is_refusal = bool(body.get("isRefusal"))
    if not is_refusal:
        return "ANSWER", None
    refusal = body.get("refusal") or {}
    code = refusal.get("code")
    return "REFUSAL", str(code) if code else "UNKNOWN"


def summarize(rows: List[Dict[str, Any]]) -> Dict[str, Any]:
    supported_total = sum(1 for r in rows if r["meta"]["is_supported_shape"])
    unsupported_total = len(rows) - supported_total

    supported_answered = sum(
        1
        for r in rows
        if r["meta"]["is_supported_shape"] and r["meta"]["outcome_class"] == "ANSWER"
    )
    unsupported_refused = sum(
        1
        for r in rows
        if (not r["meta"]["is_supported_shape"]) and r["meta"]["outcome_class"] == "REFUSAL"
    )

    no_citation_answers = 0
    internal_errors = 0
    refusal_codes: Dict[str, int] = {}

    for row in rows:
        result_body = row.get("result", {}).get("body", {})
        if row["meta"]["outcome_class"] == "ANSWER":
            citations = result_body.get("citations") or []
            if not citations:
                no_citation_answers += 1
        else:
            code = row["meta"].get("refusal_code")
            if code:
                refusal_codes[code] = refusal_codes.get(code, 0) + 1
                if code == "INTERNAL_ERROR":
                    internal_errors += 1

    return {
        "totals": {
            "prompts": len(rows),
            "supported": supported_total,
            "unsupported": unsupported_total,
            "supported_answered": supported_answered,
            "unsupported_refused": unsupported_refused,
            "supported_answer_rate": (supported_answered / supported_total) if supported_total else 0.0,
            "unsupported_refusal_rate": (unsupported_refused / unsupported_total) if unsupported_total else 0.0,
            "answers_missing_citations": no_citation_answers,
            "internal_error_count": internal_errors,
        },
        "refusal_codes": refusal_codes,
    }


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--base-url", required=True)
    parser.add_argument("--out", required=True)
    parser.add_argument("--seed", type=int, default=42)
    parser.add_argument("--supported-n", type=int, default=60)
    parser.add_argument("--unsupported-n", type=int, default=40)
    parser.add_argument("--timeout-s", type=int, default=60)
    args = parser.parse_args()

    out_path = Path(args.out)
    out_path.parent.mkdir(parents=True, exist_ok=True)
    out_path.write_text("", encoding="utf-8")

    cases = generate_panel(args.seed, args.supported_n, args.unsupported_n)
    rows: List[Dict[str, Any]] = []
    started = time.time()

    for idx, case in enumerate(cases, start=1):
        result = call_ask(args.base_url, case.question, args.timeout_s)
        body = result.get("body") if isinstance(result, dict) else {}
        outcome_class, refusal_code = classify_outcome(body if isinstance(body, dict) else {})

        row = {
            "meta": {
                "id": case.case_id,
                "shape_group": case.shape_group,
                "is_supported_shape": case.is_supported_shape,
                "question": case.question,
                "outcome_class": outcome_class,
                "refusal_code": refusal_code,
            },
            "result": result,
        }
        rows.append(row)

        with out_path.open("a", encoding="utf-8") as handle:
            handle.write(json.dumps(row, ensure_ascii=False) + "\n")

        code_suffix = f":{refusal_code}" if refusal_code else ""
        print(f"[{idx:03d}/{len(cases):03d}] {case.case_id} -> {outcome_class}{code_suffix}", file=sys.stderr)

    summary = summarize(rows)
    summary["meta"] = {
        "seed": args.seed,
        "supported_n": args.supported_n,
        "unsupported_n": args.unsupported_n,
        "base_url": args.base_url,
        "output": str(out_path),
        "duration_s": round(time.time() - started, 2),
    }
    print(json.dumps(summary, indent=2, ensure_ascii=False))


if __name__ == "__main__":
    main()
