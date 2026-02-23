# Phase 15 Maturity Checklist (Ask + Explain) — Unified Authoritative Version

This document defines **what “done” means for Phase 15**, how to test it, and which artifacts are authoritative.
It replaces earlier interpretations that relied on a fixed list of test questions as the primary completion signal.

Goal: the Ask feature is **useful, predictable, and honest** for supported descriptive analytics shapes while safely refusing unsupported or speculative requests.

---

# Phase 15 Authority Model

Phase 15 completion is determined by:

## 1️⃣ Phase 15 Exit Rubric (Primary Authority)
Location:
```
docs/phase-notes/phase15/phase15_exit_rubric.md
```

Defines:
- Supported vs unsupported shape envelope
- Safety guarantees
- Reliability thresholds
- Determinism guarantees

---

## 2️⃣ Generated Pattern Panel (Primary Test Mechanism)
Location:
```
docs/phase-notes/phase15/phase15_generated_panel_spec.md
```

Defines:
- Template-driven prompt generation
- Supported + unsupported pattern coverage
- Evaluation logic
- Determinism checks

---

## 3️⃣ 10 Question Panel (Reclassified)
The historical 10-question panel is now:

> Diagnostic smoke panel only (regression signal), not phase completion criteria.

It is still useful for:
- Demo validation
- Quick regression detection
- Developer sanity checks

It is NOT used for:
- Defining scope
- Defining capability boundaries
- Determining Phase 15 completion

---

# Scope Statement

Phase 15 Ask is a **Fact-Pack grounded explainer**.

It must:
- Answer supported descriptive analytics questions with grounded claims and citations
- Refuse unsupported intents:
  - why / cause
  - should / policy
  - will / predict / forecast
  - hypothetical scenarios
  - derived analytics not present in Fact Packs
- Never answer a different question “as if” it answered the user’s question

---

# Supported Shape Envelope (Phase 15)

Phase 15 must reliably support these **shape classes**:

## Trend (single series descriptive)
Examples:
- Explain hydro trends 2018–2023
- How has renewable generation changed since 2000?

## Overview / Mix
Examples:
- Main sources of electricity generation
- Generation mix today

## Compare (two entities descriptive)
Examples:
- Compare hydro vs geothermal trends
- Compare fuel patterns over last 20 years

## Classification / Distribution Summary
Examples:
- Regional water quality distribution
- Improving vs degrading site trends

---

# Unsupported Shape Envelope (Must Refuse)

Correct behavior = Refusal.

## Intent Categories
- Causation / Why
- Prediction / Forecasting
- Policy / Recommendations
- Hypothetical scenarios

## Phase 16 Derived Analytics (Unless Fact Pack Explicitly Contains Result)
- Ranking across many entities
- Argmax window detection (“fastest growth”)
- Share / threshold crossing analytics
- Largest change detection across arbitrary windows

---

# Acceptance Thresholds

Phase 15 is complete only when ALL gates pass.

---

## Gate A — Safety (Hard Gate — Zero Tolerance)

For every non-refusal:
- citations must be non-empty
- citations must correspond to Fact Pack evidence
- No hallucinated or uncited factual claims

For every refusal:
- isRefusal = true
- refusal.code ∈
  - UNSUPPORTED_INTENT
  - UNABLE_TO_PARSE
  - UNSUPPORTED_CAPABILITY
  - DATASET_MISMATCH
  - MISSING_REQUIRED_FILTERS

System-wide:
- No INTERNAL_ERROR responses visible to the user

---

## Gate B — Reliability (Pattern-Level Coverage)

Measured using Generated Pattern Panel.

Pass criteria:
- Supported prompts → ≥ 70% answered
- Unsupported prompts → ≥ 95% correctly refused

Supported prompts may refuse ONLY for:
- MISSING_REQUIRED_FILTERS
- UNABLE_TO_PARSE (investigate if > 5%)

---

## Gate C — Determinism / Repeatability

Run Generated Pattern Panel 3 times (fresh app each run).

Pass criteria:
- Each prompt yields same outcome class each run:
  - ANSWER vs REFUSAL
- If refusal → same refusal code each run
- No INTERNAL_ERROR across runs

---

# Detailed Testing Instructions

## Environment Setup
- LLM configured and reachable
- Backend + frontend running
- `/api/v1/explanations/capabilities` returns supported shapes and datasets

---

## Automated Test Runs

Backend:
```
./mvnw -f backend/pom.xml test
```

Frontend (if applicable):
```
npm test
```

---

## Generated Pattern Panel Runs
Run harness three times and store outputs:

```
out_run1.txt
out_run2.txt
out_run3.txt
```

---

## Panel Evaluation

Per run:
- No INTERNAL_ERROR outcomes
- All non-refusals include citations
- Unsupported prompts correctly refused

Across runs:
- Stable outcome class
- Stable refusal codes

---

## Debug Workflow

Check logs for:
- Parsed intent output
- Dataset selection reasoning
- Fact pack shape contents
- Citation validation required vs actual

---

# Phase 16 Boundary Reminder

Treat as Phase 16 unless Fact Packs explicitly contain computed results:

- Ranking across fuels
- Argmax windows
- Share of total generation
- Threshold crossing detection

---

# Required Phase 15 Hardening (Must Exist Before Signoff)

1. Citation family-prefix validation
2. Deterministic required citation selection
3. Derived analytics refusal triggers

Reference:
```
docs/archive/phase15/2026-02-doc-convergence/phase15_builder_task_brief.md (historical brief)
```

---

# Final Phase 15 Signoff Statement

Phase 15 is complete when the system can truthfully state:

> The Ask feature reliably answers supported descriptive analytics questions grounded in official datasets, safely refuses unsupported or speculative questions, and never produces uncited factual claims.

