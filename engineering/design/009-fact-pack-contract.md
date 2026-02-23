# Wai & Watts — Fact Pack Contract (Phase 11)

**Status:** Draft v1 (Architecture Contract)  
**Phase:** 11 — Insights & LLM Layer (Grounded Explanations)  
**Owner:** Staff / Strategy (Architecture)  
**Implementer:** Builder GPT (after this contract is accepted)

---

## 1. Purpose

A **Fact Pack** is the **only allowed data interface** between Wai & Watts domain data and any LLM.

A Fact Pack is a **deterministic, structured, provenance-safe bundle of facts derived from the database** that the LLM may use to generate natural-language explanations.

Fact Packs enforce:

- **Grounded explanations only** (database remains source of truth)
- **No hallucinated data** (LLM can’t invent missing fields)
- **Deterministic inputs** (same DB + same request → same Fact Pack)
- **Explicit provenance** (dataset_source + dataset_release + content_hash)
- **Reproducible behavior** (explanations can be tested/verified)

The LLM is an explanation engine only — it must not become an authority.

---

## 2. Architectural Role

Fact Packs sit between persisted facts and the explanation output:

```
DB queries
  → FactPackBuilder (deterministic)
  → FactPack JSON
  → ExplanationService
  → LLM Provider Adapter
  → Explanation (with citations to FactPack fields)
```

**LLMs MUST NEVER:**
- Query the database
- See ORM entities
- See raw publisher artifacts
- Compute metrics not explicitly present in the Fact Pack
- “Fill in” missing facts by inference

---

## 3. Core Invariants (Non-Negotiable)

### 3.1 Deterministic
Same DB state + same request inputs → identical Fact Pack output (including ordering).

### 3.2 Complete
If an explanation requires a field, that field must be included in the Fact Pack.
The LLM must not derive new facts.

### 3.3 Provenance-safe
Every Fact Pack must include:
- dataset_source_code
- dataset_release_id(s)
- content_hash(es)

### 3.4 Read-only and ephemeral
Fact Packs are generated per request and are not persisted.

---

## 4. Fact Pack Top-Level Schema

### 4.1 Canonical Structure (JSON)

```json
{
  "fact_pack_version": "1.0",
  "generated_at_utc": "2026-02-06T21:00:00Z",

  "request_context": {
    "question_type": "string",
    "dataset_scope": ["string"],
    "filters_applied": { "k": "v" }
  },

  "provenance": {
    "dataset_sources": [
      {
        "dataset_source_code": "string",
        "dataset_release_id": "string",
        "content_hash": "string",
        "period_coverage": "string"
      }
    ]
  },

  "facts": {
    "metrics": [],
    "comparisons": [],
    "time_series": [],
    "classifications": []
  },

  "guardrails": {
    "allowed_claims": ["string"],
    "forbidden_claims": ["string"],
    "required_citations": ["string"]
  }
}
```

---

## 5. Provenance Schema

### 5.1 DatasetSourceProvenance

```json
{
  "dataset_source_code": "mbie.generation.annual",
  "dataset_release_id": "release_2025_01_15",
  "content_hash": "sha256:abc123...",
  "period_coverage": "1974–2024"
}
```

**Rules:**
- `dataset_source_code` must match the canonical dataset source taxonomy.
- `dataset_release_id` must refer to the lineage release for the facts included.
- `content_hash` must be the release hash used for idempotency (canonical).
- `period_coverage` is descriptive and must be derived deterministically.

---

## 6. Fact Types

The Fact Pack is a bundle of typed fact objects.  
LLM outputs must cite these objects by stable paths/ids.

### 6.1 MetricFact

```json
{
  "id": "metric:mbie:generation_gwh:2022:HYDRO",
  "metric_name": "generation_gwh",
  "value": 26071,
  "unit": "GWh",
  "period": "2022",
  "dimensions": { "fuel_type": "HYDRO" }
}
```

### 6.2 ComparisonFact

```json
{
  "id": "cmp:mbie:generation_gwh:HYDRO:2022_vs_2021",
  "metric_name": "generation_gwh",
  "baseline_period": "2021",
  "comparison_period": "2022",
  "delta_absolute": 123,
  "delta_percent": 0.47,
  "unit": "GWh",
  "dimensions": { "fuel_type": "HYDRO" }
}
```

### 6.3 TimeSeriesFact

```json
{
  "id": "ts:mbie:renewable_generation_gwh:2010_2024",
  "metric_name": "renewable_generation_gwh",
  "unit": "GWh",
  "dimensions": { "scope": "NZ" },
  "points": [
    { "period": "2018", "value": 42000 },
    { "period": "2024", "value": 47000 }
  ]
}
```

### 6.4 ClassificationFact (LAWA-style)

```json
{
  "id": "cls:lawa:state:site:XYZ:2019_2023",
  "subject": "site:XYZ",
  "classification_type": "STATE",
  "classification": "GOOD",
  "period_start_year": 2019,
  "period_end_year": 2023,
  "dimensions": { "indicator": "NITRATE" }
}
```

---

## 7. Guardrails (Critical)

The Fact Pack must include explicit guardrails that the LLM must obey.

### 7.1 allowed_claims
A whitelist of claim types the LLM may produce (examples):
- `trend_increase`
- `trend_decrease`
- `largest_contributor`
- `classification_summary`

### 7.2 forbidden_claims
A blacklist of claim types the LLM must refuse (examples):
- `forecast`
- `causation`
- `policy_recommendation` (unless explicitly defined later)
- `new_metric_computation`

### 7.3 required_citations
A list of Fact Pack `id`s or JSON paths that must be cited in the answer.

**Minimum requirement:** Every factual statement must be supported by one or more citations to Fact Pack facts.

---

## 8. Prompting Contract (LLM Input)

LLM input must include, in this order:

1) User question  
2) Fact Pack JSON (verbatim)  
3) Instruction block:

- “Use only the facts provided.”
- “Do not infer missing facts.”
- “If information is missing, refuse.”
- “Every factual claim must cite Fact Pack ids/paths.”

**The Fact Pack is the source of truth for the LLM.**

---

## 9. Refusal Contract

The LLM must refuse when:
- The Fact Pack lacks required facts to answer the question
- The question requests forecasting or causation
- The question is outside dataset scope
- The question requires computations not present in Fact Pack

Standard refusal format:

> I can’t answer that using the available dataset facts.  
> If you want, I can explain what facts would be needed.

Refusals are a success condition (not a failure).

---

## 10. Builder Responsibilities and Boundaries

### 10.1 Fact Pack Builders (dataset-specific)
Phase 11 requires dataset-specific builders:

- `MbieGenerationAnnualFactPackBuilder`
- `MbieGenerationQuarterlyFactPackBuilder`
- `LawaStateFactPackBuilder`
- `LawaTrendFactPackBuilder`

**Rules:**
- Builders query DB only.
- Builders do not call LLMs.
- Builders do not contain explanation prose.
- Builders are deterministic (stable ordering, stable IDs).
- Builders must include provenance fields.

### 10.2 Explanation Service
- Accepts a request (question type + filters)
- Selects the appropriate FactPackBuilder(s)
- Produces a Fact Pack
- Calls LLM Provider Adapter
- Returns explanation + citations

**Explanation logic lives here, not in builders.**

### 10.3 LLM Provider Adapter
- Pluggable provider interface (single provider first is fine)
- Responsible for:
  - serializing Fact Pack + instructions
  - receiving response
  - validating citations presence (best-effort)
- Must not include DB logic

---

## 11. Versioning

Fact Pack schema includes:

- `fact_pack_version` (e.g., `"1.0"`)

Any breaking schema change requires:
- incrementing `fact_pack_version`
- updating tests
- documenting change rationale

---

## 12. Testing Requirements (Phase 11)

At minimum:

### 12.1 Determinism test
Same DB fixture + same inputs → identical Fact Pack JSON.

### 12.2 Completeness test
Fact Pack contains required fields for a supported question type.

### 12.3 Refusal path test
Missing/unsupported facts → deterministic refusal.

### 12.4 Citation requirement test (best-effort)
LLM output must reference required Fact Pack ids/paths (if not, treat as invalid output and return a refusal/fallback).

---

## 13. Phase 11 Non-Goals

Fact Packs will NOT:
- Include raw DB rows or entities
- Include SQL strings
- Include publisher artifacts
- Include natural language summaries
- Include predictions/forecasts
- Compute domain metrics unless explicitly included as Fact Pack fields (computed in builders)

---

## 14. Example Minimal Fact Pack

```json
{
  "fact_pack_version": "1.0",
  "generated_at_utc": "2026-02-06T21:00:00Z",
  "request_context": {
    "question_type": "renewable_generation_trend",
    "dataset_scope": ["mbie.generation.annual"],
    "filters_applied": { "scope": "NZ" }
  },
  "provenance": {
    "dataset_sources": [
      {
        "dataset_source_code": "mbie.generation.annual",
        "dataset_release_id": "release_2025_01_15",
        "content_hash": "sha256:abc123...",
        "period_coverage": "2018–2024"
      }
    ]
  },
  "facts": {
    "metrics": [],
    "comparisons": [],
    "time_series": [
      {
        "id": "ts:mbie:renewable_generation_gwh:2018_2024",
        "metric_name": "renewable_generation_gwh",
        "unit": "GWh",
        "dimensions": { "scope": "NZ" },
        "points": [
          { "period": "2018", "value": 42000 },
          { "period": "2024", "value": 47000 }
        ]
      }
    ],
    "classifications": []
  },
  "guardrails": {
    "allowed_claims": ["trend_increase"],
    "forbidden_claims": ["forecast", "causation"],
    "required_citations": ["facts.time_series[0].id"]
  }
}
```

---

## 15. Phase 11 Success Criteria

Phase 11 is successful when:
- Explanations are fully grounded in Fact Packs
- LLM never queries DB, never infers missing facts
- Refusals are deterministic and documented
- The system is testable without relying on “prompt magic”
- Fact Packs are stable and versioned

