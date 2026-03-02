# Spec 017 — Capability Declaration & NL Determinism

Status: Proposed  
Owner: Staff Engineer  
Phase: 17  
Last updated: 2026-02-25  

---

## 1. Purpose

Phase 16 successfully established the explanation boundary and capability-driven UI.

However, two risks remain:

1. Natural language parsing is not fully deterministic.
2. Capability definitions still rely on loosely structured string values, making expansion harder and increasing the risk of hidden hard-coding.

Phase 17 formalizes capabilities as a declared, testable contract and ensures deterministic natural language behavior.

This phase strengthens architectural integrity without expanding system scope.

---

## 2. Goals

### Primary goals

• Make capabilities fully declared and structured internally  
• Ensure NL parsing produces deterministic structured requests  
• Guarantee `/api/v1/capabilities` remains the single source of truth  
• Prevent regressions through CI contract and determinism tests  

### Secondary goals

• Improve internal clarity using enums  
• Enable clean future expansion of supported questions  
• Increase reviewer confidence in system robustness  

---

## 3. Non-Goals

This phase does NOT:

• Add new datasets  
• Add new explanation types  
• Add vector search, embeddings, or ML classification  
• Replace capability registry with enums  

Enums are organizational, not authoritative.

---

## 4. Capability declaration model

Capabilities remain declared in a central registry.

Capabilities define:

• questionType  
• datasetSource  
• required filters  
• optional filters  
• suggestedValuesByToken  
• examples  

The registry remains the authoritative support matrix.

Enums provide structured vocabulary.

---

## 5. Internal enum model

Add enums for structured internal representation.

Example:

QuestionType  
DatasetSource  
MetricType  
TrendDirection  
StateCategory  

Each enum provides:

• wire value string  
• display name  
• optional metadata  

Example:

FUEL_GENERATION_TREND("fuel_generation_trend")

DTOs continue using string wire values.

Mapping layer converts between strings and enums.

---

## 6. Parser normalization contract

Parser must produce normalized requests before validation.

Normalization rules:

• Remove "unknown" categorical values  
• Normalize casing and whitespace  
• Default missing required categorical filters when capability defines a single valid value  
• Remove empty filter values  
• Ensure datasetSource present  

Parser MUST NOT emit invalid categorical tokens.

---

## 7. Capability-constrained validation

After normalization:

Request must match a declared capability signature.

If no matching capability exists:

System refuses with:

UNSUPPORTED_CAPABILITY

and provides capability guidance.

System MUST NOT silently reinterpret questions.

---

## 8. Determinism testing

Add CI test suite:

Fixed prompt corpus (minimum 20 prompts).

Each prompt executed multiple times.

Test asserts:

identical structured request  
identical outcome  
identical refusal category  

Test fails on drift.

---

## 9. Capability contract tests

Add tests asserting `/api/v1/capabilities` contract stability.

Verify presence of:

questionType  
datasetSource  
filter schema  
suggestedValuesByToken  
examples  

Test protects frontend contract.

---

## 10. Observability

Add metrics for each stage:

parse  
validation  
fact pack selection  
explanation generation  
citation validation  

Track refusal codes.

---

## 11. Governance documentation

Document:

branch protection  
required checks  
review requirements  

Ensure governance is visible.

---

## 12. Acceptance criteria

Phase complete when:

NL determinism tests pass  
Capabilities contract tests pass  
Enums introduced without breaking API  
Parser normalization implemented  
No new hard-coded capability logic introduced  

---

## 13. Future capability expansion (reserved)

This section intentionally reserved.

Future additions may include:

additional MBIE question types  
additional LAWA comparisons  
degrading site queries  
regional comparisons beyond current scope  

Capability registry allows expansion without architecture changes.

---

## 14. Risks

Primary risk:

Enum misuse becoming a second capability registry.

Mitigation:

Capability registry remains authoritative.

Enums represent vocabulary only.

---

## 15. Success definition

System capabilities are declarative, discoverable, deterministic, and safely extensible.
