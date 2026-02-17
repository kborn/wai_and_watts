# Wai & Watts — Explanation Request Contract (Phase 11/12)

**Status:** Draft v1 (Contract)  
**Applies to:** Phase 11 (structured explanations) + Phase 12 (NL intent parsing)  
**Authority:** Must remain consistent with `progress.md` guardrails (NL is routing only) and Fact Pack boundary.

This document defines the **canonical structured request** that drives:
- Fact Pack selection
- Fact Pack construction
- Explanation rendering/refusal

> The Natural Language layer in Phase 12 may ONLY produce this structure.
> It may not invent new fields, question types, or filter semantics.

---

## 1. Canonical JSON Shape

```json
{
  "questionType": "renewable_generation_trend",
  "datasetSource": "mbie.generation.annual",
  "filters": {
    "startYear": 2018,
    "endYear": 2024,
    "fuelType": "WIND",
    "indicator": "NITRATE",
    "region": "Waikato",
    "trend": "IMPROVING"
  }
}
```

### 1.1 Required fields
- `questionType` *(string, required)* — must be one of **Supported Question Types** (Section 2)
- `datasetSource` *(string, required)* — must be one of **Supported Dataset Sources** (Section 3)

### 1.2 Optional field
- `filters` *(object, optional)* — schema is **question-type-specific** and validated (Section 4)

---

## 2. Supported Question Types (Canonical)

These values are **exact** and **case-sensitive**.

### MBIE Generation
- `renewable_generation_trend` — Explain renewable generation trends between years
- `hydro_generation_trend` — Explain hydro generation trends between years
- `fuel_type_comparison` — Compare two fuel types (e.g., hydro vs geothermal)
- `generation_mix_overview` — Summarize main sources of electricity generation

### LAWA Water Quality State
- `water_quality_overview` — Overview of water quality state distribution
- `excellent_sites_trend` — Trends in excellent water quality sites
- `regional_water_quality` — Compare water quality across regions

### LAWA Water Quality Trend
- `water_quality_trends` — Overall trend distribution
- `improving_sites_trend` — Trends in improving sites
- `regional_trend_comparison` — Compare trends across regions

> **Rule:** Phase 12 NL parsing MUST map only to one of the above strings.
> Unknown or “close enough” values MUST be refused.

---

## 3. Supported Dataset Sources (Canonical)

- `mbie.generation.annual`
- `mbie.generation.quarterly`
- `lawa.water_quality.state.multi_year`
- `lawa.water_quality.trend.multi_year`

> **Rule:** `datasetSource` is required and must match one of these exact codes.

---

## 4. Filter Validation Contract

### 4.1 Filter schema (allowed keys)

Allowed keys (global superset):
- `fuelType` *(string, MBIE only)*
- `fuelTypeB` *(string, MBIE only; optional second fuel for comparison)*
- `indicator` *(string, LAWA only)*
- `region` *(string, LAWA only)*
- `trend` *(string, LAWA trend only)*
- `startYear` *(integer, optional)*
- `endYear` *(integer, optional)*

Unknown filter keys MUST be refused (not ignored).

### 4.2 Cross-field rules (global)
- If both `startYear` and `endYear` are present: `startYear <= endYear`
- If only one is present: permitted
- Missing filters are permitted unless the question type requires them

### 4.3 Question-type specific constraints (minimum contract)

**MBIE question types**
- `datasetSource` must be `mbie.generation.annual` or `mbie.generation.quarterly`
- `fuelType` is:
  - **required** for `fuel_type_comparison` (use `fuelType` + `fuelTypeB` to compare two fuels)
  - optional for `renewable_generation_trend` and `hydro_generation_trend`
  - optional for `generation_mix_overview`

**LAWA state question types**
- `datasetSource` must be `lawa.water_quality.state.multi_year`
- `indicator` and `region` are optional (unless a future spec tightens)

**LAWA trend question types**
- `datasetSource` must be `lawa.water_quality.trend.multi_year`
- `trend` optional unless the question type implies it (e.g., `improving_sites_trend` may default trend=IMPROVING if and only if that’s already how Phase 11 structured requests behave)

> If any of the above constraints are violated → refuse deterministically (see `0112-refusal-taxonomy.md`).

---

## 5. API Endpoints (Contract-Level)

Structured endpoint (canonical, Phase 11+):
- `POST /api/v1/explanations` — accepts the JSON above.

Natural language endpoint (Phase 12):
- `POST /api/v1/explanations/ask` — accepts `{ "question": "..." }` and returns:
  - `parsedRequest` (the canonical request)
  - plus the normal explanation payload

> The `/ask` endpoint MUST still use the same validation/refusal behavior as `/explanations`.

---

## 6. Versioning

This request contract is intentionally small. If it grows:
- Add `schemaVersion` at top-level (optional), or
- Version the endpoint explicitly

Any breaking change must be recorded in decisions.md and accompanied by tests.
