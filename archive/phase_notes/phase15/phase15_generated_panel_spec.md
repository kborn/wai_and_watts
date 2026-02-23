# Wai & Watts — Phase 15 Generated Pattern Panel Spec

**Goal:** Replace “static list of questions” with a stable, repeatable generator that validates shape coverage and safety.

This spec defines:
- prompt templates
- substitution domains
- evaluation rules
- output format expectations

---

## 1) Prompt Template Library

### A) Supported templates

#### A1 — Trend (single series)
- "Explain {fuel} generation trends between {startYear} and {endYear}."
- "How has {fuel} generation changed since {startYear}?"
- "Describe renewable generation trends between {startYear} and {endYear}."

#### A2 — Overview / mix
- "What are the main sources of electricity generation in New Zealand?"
- "What does the generation mix look like in {year}?"
- "Summarize electricity generation mix today."

#### A3 — Compare (two entities)
- "Compare {fuelA} and {fuelB} generation patterns."
- "Compare {fuelA} vs {fuelB} trends over the last {nYears} years."
- "Has {fuelA} increased faster than {fuelB} in recent years?"  *(Phase 15 note: must answer descriptively; if it implies an argmax/ranking, refuse)*

#### A4 — LAWA distribution / classification summary
- "What does river water quality look like across New Zealand right now?"
- "Which regions have the highest proportion of poor river water quality sites?"
- "Are more river sites improving or degrading in water quality over time?"

---

### B) Unsupported templates (must refuse)

#### B1 — Causation
- "Why did {fuel} generation drop in {year}?"
- "What caused river quality to decline in {region}?"

#### B2 — Forecast / prediction
- "Will {fuelA} overtake {fuelB} by {year}?"

#### B3 — Policy
- "Should NZ invest more in {fuel}?"

#### B4 — Derived analytics (Phase 16)
- "Which fuel has grown the most since {startYear}?"
- "In what {window}-year period did NZ have the biggest increase in {fuel} generation?"
- "When did renewables first exceed {threshold}% of total generation?"

---

## 2) Substitution Domains

### Datasets
- MBIE annual: `mbie.generation.annual`
- MBIE quarterly: `mbie.generation.quarterly`
- LAWA state: `lawa.water_quality.state.multi_year`
- LAWA trend: `lawa.water_quality.trend.multi_year`

### Fuels (MBIE)
- HYDRO, WIND, SOLAR, GEOTHERMAL, COAL, GAS

### Years / windows
- startYear: 2000, 2005, 2010, 2018, 2020
- endYear: 2023, 2024
- year: 2024
- nYears: 10, 20
- window: 3, 5, 10
- threshold: 80

### Regions (LAWA)
Use a small stable set to avoid “string mismatch” noise:
- Auckland
- Waikato
- Canterbury
- Otago
- Bay of Plenty

---

## 3) Generation Rules

### Minimal recommended panel size
- Supported prompts: 60
- Unsupported prompts: 40
Total: 100 prompts per run

### Sampling approach
- Always include at least:
  - 10 trend prompts (MBIE)
  - 10 compare prompts (MBIE)
  - 10 overview prompts (MBIE)
  - 10 LAWA prompts (state/trend)
- Remaining prompts are random expansions from templates
- Randomness must be seedable (`--seed`) so a failing run can be reproduced exactly

---

## 4) Evaluation Rules

### Per-response checks
For each non-refusal:
- `citations` must be non-empty
- citation IDs must be valid Fact Pack IDs
- response must match shape:
  - If prompt is derived analytics (argmax/ranking/share), response must not “pretend” to answer; it must refuse

For each refusal:
- `isRefusal=true`
- refusal code ∈ {UNSUPPORTED_INTENT, UNABLE_TO_PARSE, UNSUPPORTED_CAPABILITY, DATASET_MISMATCH, MISSING_REQUIRED_FILTERS}

### Panel-level thresholds
- Supported prompts: ≥70% answered
- Unsupported prompts: ≥95% refused as `UNSUPPORTED_INTENT` or `UNSUPPORTED_CAPABILITY`
- Across 3 runs: outcome class stable; no INTERNAL_ERROR

---

## 5) Output Format

The harness should write a machine-readable JSONL or JSON array with, per prompt:
- `question`
- `parsedRequest` (or null)
- `selectedDatasetSource`
- `isRefusal`
- `refusal.code`
- `citations` (array)
- `debug.parserUsed`, `debug.refusalTrigger` (if present)

Additionally, emit a summary block:
- totals: supported/unsupported counts
- answered/refused counts by shape
- refusal-code distribution
- any INTERNAL_ERROR count
- determinism report across runs

