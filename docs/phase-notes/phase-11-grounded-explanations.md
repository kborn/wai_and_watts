# Wai & Watts — Phase 11 Notes: Grounded Explanations

## Status
Phase: 11 — Grounded Explanation Layer  
Purpose: Provide deterministic, citation-backed natural language explanations over persisted dataset facts.

---

## Overview

Phase 11 introduces a grounded explanation system built on **Fact Packs**.

Fact Packs are the only data interface between persisted domain data and any explanation provider (stub or future LLM). This guarantees explanations are traceable to database facts with citation validation and provenance metadata.

This phase proves that AI can be integrated as a **bounded rendering subsystem**, not as a data source or reasoning authority.

---

## Core Architectural Principles

### Fact Pack as the Only LLM Interface
- Providers never access repositories or entities directly
- All explanation inputs are deterministic and structured
- All responses must cite Fact Pack facts
- All facts include lineage metadata

---

### Deterministic Explanation Pipeline

```
Request
 → Controller
 → Explanation Service
 → Fact Pack Builder
 → Fact Pack
 → Explanation Provider (Stub in Phase 11)
 → Response
```

Providers render text.  
They do not compute facts or perform data discovery.

---

## Core System Components

### Fact Pack Builders
Responsible for querying persisted data and producing validated Fact Packs.

Examples include:
- MBIE electricity generation (annual / quarterly)
- LAWA water quality (state / trend)

Builders:
- Query DB only
- Compute derived metrics deterministically
- Attach provenance metadata
- Never call explanation providers

---

### Explanation Service
Orchestrates:
- Request validation
- Fact Pack construction
- Refusal evaluation
- Provider invocation
- Citation enforcement

---

### Explanation Provider (Phase 11)
Phase 11 uses a deterministic stub provider to validate:
- Fact Pack correctness
- Citation enforcement
- Refusal handling
- Response structure stability

Future providers must obey the same contract.

---

### API Layer

The explanation API accepts **structured requests**, not freeform prompts.

Example:

```json
POST /api/v1/explanations
{
  "questionType": "renewable_generation_trend",
  "filters": {
    "datasetSource": "mbie.generation.annual"
  }
}
```

---

### Response Format

#### Success
```json
{
  "explanationText": "...",
  "citations": ["fact_id"],
  "isRefusal": false
}
```

#### Refusal
```json
{
  "explanationText": "...",
  "citations": [],
  "isRefusal": true,
  "refusalReason": "..."
}
```

---

## Refusal Model

Requests are refused when:
- Question type is unsupported
- Required facts are missing
- Dataset is unavailable
- Citation validation fails

Refusals are considered correct behavior.

---

## Development Constraints

### No Freeform Prompting
Only structured question types are supported.

### Fact Pack First
All data must pass Fact Pack validation before explanation.

### Citation Required
All factual statements must map to Fact Pack facts.

### No Hallucinated Facts
Providers cannot introduce new values.

---

## Testing Philosophy

Testing focuses on:
- Fact Pack correctness and determinism
- Citation enforcement
- Refusal correctness
- End-to-end request handling

The goal is stable, reproducible explanation behavior independent of provider implementation.

---

## Extensibility

Future phases may add:
- Additional dataset Fact Pack builders
- Additional question types
- Real provider integration behind the same adapter
- Enhanced citation validation

All future work must preserve:
- Fact Pack boundary
- Deterministic data preparation
- Provider isolation

---

## Phase 11 Outcome

After Phase 11:
- Explanations are grounded in persisted data
- The system supports structured explanation requests
- Providers are fully replaceable rendering dependencies
- The architecture prevents hallucination through design, not prompting
