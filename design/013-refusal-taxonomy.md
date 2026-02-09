# Wai & Watts — Refusal Taxonomy (Phase 11/12)

**Status:** Draft v1  
**Applies to:** Structured explanations (Phase 11) + NL parsing (Phase 12)  
**Purpose:** Prevent “guessing” and keep refusal behavior deterministic and testable.

---

## 1. Refusals are Correct Behavior

A refusal is a success condition when:
- the request is unsupported
- the request is ambiguous
- required facts are unavailable
- prohibited claim types are requested (forecasting, causation, etc.)

---

## 2. Canonical Refusal Categories

### A) `UNSUPPORTED_QUESTION_TYPE`
Use when:
- `questionType` not in the supported set

Example:
- Parser outputs `renewables_mix_story` → refuse

---

### B) `UNSUPPORTED_REQUEST_CLASS`
Use when the user asks for something explicitly out of scope, such as:
- forecasting / prediction
- causal claims
- policy recommendations
- hypotheticals / counterfactuals
- site-specific advice (health/safety)

These align with the “unsupportedQuestionTypes” list exposed by the system.

---

### C) `INVALID_FILTERS`
Use when:
- unknown filter keys are provided
- filter types are wrong (e.g., startYear = "two thousand")
- cross-field constraints violated (startYear > endYear)
- filters incompatible with questionType/datasetSource

---

### D) `AMBIGUOUS_INTENT`
Use when:
- NL cannot be mapped confidently to a single supported question type
- essential request fields are missing and cannot be inferred safely
- multiple plausible interpretations exist

---

### E) `INSUFFICIENT_FACTS`
Use when:
- the request is valid, but the Fact Pack builder cannot produce the necessary facts
- dataset coverage doesn’t include the requested period/filters
- required comparisons/metrics were not produced in the Fact Pack

---

## 3. Standard Refusal Response Shape (API)

Recommended response JSON (stable for UI):

```json
{
  "ok": false,
  "refusal": {
    "category": "AMBIGUOUS_INTENT",
    "message": "I can’t confidently map this question to a supported explanation type.",
    "howToFix": "Try specifying the dataset and a time range. Example: 'Explain renewable generation trends between 2018 and 2024 (MBIE annual)'.",
    "supportedQuestionTypesEndpoint": "/api/v1/explanations/supported-question-types"
  }
}
```

Notes:
- Keep `message` human-friendly and short.
- `howToFix` is optional but useful for UX.
- Link to supported types endpoint for discoverability.

---

## 4. Determinism Rules

- A given invalid input should map to the same refusal category every time.
- Do not “try harder” with additional LLM calls.
- Do not silently coerce invalid filters.

---

## 5. Phase 12 Test Matrix (Minimum)

1) Unsupported NL request (forecast) → `UNSUPPORTED_REQUEST_CLASS`  
2) Ambiguous NL request → `AMBIGUOUS_INTENT`  
3) Valid NL request → ok (goes through fact pack + explanation)  
4) Invalid filter structure → `INVALID_FILTERS`  
5) Valid structured request but no DB data in range → `INSUFFICIENT_FACTS`
