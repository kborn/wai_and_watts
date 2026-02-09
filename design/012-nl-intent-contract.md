# Wai & Watts — Natural Language Intent Parsing Contract (Phase 12)

**Status:** Draft v1 (Contract)  
**Phase:** 12 — Natural Language Query Interface + Thin Frontend  
**Purpose:** Ensure NL parsing is **routing-only** and cannot violate Fact Pack grounding.

---

## 1. What the Intent Parser IS

The intent parser converts:

> Natural language user question → **validated** `ExplanationRequest` (per `0110-explanation-request-contract.md`)

It may:
- Select `questionType`
- Select `datasetSource`
- Extract filters (`startYear`, `endYear`, `fuelType`, `indicator`, `region`, `trend`)

---

## 2. What the Intent Parser is NOT (Hard Prohibitions)

The intent parser must NOT:
- Generate facts, metrics, or explanations
- Access the database
- Use external knowledge as factual claims
- Invent new question types
- Guess when intent is ambiguous

If ambiguous → refusal.

---

## 3. NL Input / Output Shapes

### 3.1 Input to `/api/v1/explanations/ask`
```json
{ "question": "How did renewable generation change from 2018 to 2024?" }
```

### 3.2 Required Output from parser (internal)
Must be exactly the canonical shape:

```json
{
  "questionType": "renewable_generation_trend",
  "datasetSource": "mbie.generation.annual",
  "filters": { "startYear": 2018, "endYear": 2024 }
}
```

No extra keys.

---

## 4. Parser Output Validation (Mandatory)

Before the request reaches the Explanation pipeline, validate:
- `questionType` in supported set
- `datasetSource` in supported set
- Filters:
  - allowed keys only
  - cross-field rules (e.g., startYear <= endYear)
  - question-type compatibility rules

Validation failure behavior:
- deterministic refusal (see `0112-refusal-taxonomy.md`)
- never “best effort” fixes

---

## 5. Ambiguity Rules (Minimum)

If any of the following hold, refuse with reason = `AMBIGUOUS_INTENT`:
- Multiple plausible question types with no clear winner
- Missing required field (e.g., datasetSource) that cannot be inferred *safely*
- Conflicting filters (e.g., MBIE questionType + LAWA datasetSource)

Optional (future): confidence scoring, but not required in Phase 12.

---

## 6. Prompt Contract for LLM-based Parsing

If the parser uses an LLM, the prompt must:
- Enumerate allowed `questionType` values
- Enumerate allowed `datasetSource` values
- Enumerate allowed filter keys
- Explicitly instruct: output **JSON only** matching the schema
- Explicitly instruct: if ambiguous, output a refusal object (see below)

### 6.1 Recommended parser response schema (LLM output)

Either:

**A) success**
```json
{
  "ok": true,
  "request": {
    "questionType": "renewable_generation_trend",
    "datasetSource": "mbie.generation.annual",
    "filters": { "startYear": 2018, "endYear": 2024 }
  }
}
```

**B) refusal**
```json
{
  "ok": false,
  "refusal": {
    "category": "AMBIGUOUS_INTENT",
    "message": "I can’t confidently map this question to a supported question type. Try one of the example questions.",
    "suggestedExampleQuestions": [
      "Explain renewable generation trends between 2018 and 2024.",
      "Compare wind vs hydro generation in 2022."
    ]
  }
}
```

> Note: This is the *parser output*, not the final API response.
> The service can translate refusal into the API refusal format.

---

## 7. Stateless UI Contract (Reminder)

Phase 12 NL UX is **single-turn and stateless**:
- No conversation history
- No follow-up questions
- Every request stands alone

---

## 8. Test Expectations (Phase 12)

Minimum:
- NL → parsed request → validation → fact pack → explanation
- NL ambiguous → deterministic refusal
- NL unsupported question class (forecast/causation/etc.) → deterministic refusal
