# Wai & Watts — Phase 15 Exit Rubric (Ask + Explain)

**Purpose:** Define “done” for Phase 15 without maintaining a static list of questions.

Phase 15 is **not** “every reasonable natural language question must work.”
Phase 15 **is**: the system reliably answers **supported shapes**, reliably refuses **unsupported shapes**, and never produces ungrounded claims.

This rubric is **authoritative** for Phase 15 completion and replaces “10 questions must work” as the completion mechanism.
The 10-question panel remains as a **diagnostic smoke panel** (regression signal), not the phase definition.

---

## 1) Phase 15 Scope Envelope

### Supported shapes (Phase 15 MUST support)
These are **shape classes**; phrasing may vary.

1. **Trend (single series, descriptive)**
   - Examples: “Explain hydro trends 2018–2023”, “How has renewable generation changed since 2000?”
2. **Overview / Mix (single period descriptive summary)**
   - Examples: “What are the main sources of generation?”, “Generation mix today”
3. **Compare (two-entity comparison; descriptive)**
   - Examples: “Compare hydro vs geothermal over the last 20 years”
4. **Classification / Distribution summary (LAWA state/trend descriptive)**
   - Examples: “What does water quality look like across regions?”, “Are more sites improving or degrading?”

### Unsupported shapes (Phase 15 MUST refuse)
Refusal is correct behavior.

- **Causation / explanation-of-why**: “Why did hydro drop in 2012?”
- **Prediction / forecasting**: “Will solar overtake wind by 2030?”
- **Policy / recommendation**: “Should NZ invest more in geothermal?”
- **Hypotheticals / counterfactuals**
- **Derived analytics not present in Fact Packs (Phase 16)**
  - Ranking across many entities: “Which fuel grew the most since 2005?”
  - Argmax windows: “When did wind grow fastest over any 3-year period?”
  - Shares / thresholds: “When did renewables exceed 80% of total?”

> **Guardrail:** The system must not answer a different question “as if” it answered the user’s question.
> If the user asks for a derived analytic (ranking/argmax/share/threshold) and the Fact Pack does not include that computed result, the correct outcome is refusal (`CAPABILITY_UNSUPPORTED`) or a structured “limitations” response with explicit refusal to compute.

---

## 2) Phase 15 Acceptance Gates

Phase 15 is **done** only when **all** gates pass.

### Gate A — Safety (hard gate)
**Must be true (0 tolerance):**
- **No uncited answers:** every non-refusal response has **non-empty** citations.
- **No hallucinated claims:** every factual claim must be supportable by citations returned.
- **No INTERNAL_ERROR responses** visible to the user.

### Gate B — Supported-shape usefulness (coverage gate)
Run the **Generated Pattern Panel** (defined below).

Pass criteria:
- **Supported prompts:** ≥ **70%** return **non-refusal** answers.
- Supported prompts that refuse must be limited to:
  - `MISSING_REQUIRED_FILTERS`
  - `UNABLE_TO_PARSE` (rare; investigate if >5%)
- **Unsupported prompts:** ≥ **95%** refuse with `UNSUPPORTED_INTENT` (or `CAPABILITY_UNSUPPORTED` for Phase-16-style derived analytics prompts).

### Gate C — Determinism / Repeatability (stability gate)
Run the Generated Pattern Panel **3 times** (fresh app start each run).

Pass criteria:
- Each prompt yields the same **outcome class** each run:
  - `ANSWER` vs `REFUSAL`
- If refusal:
  - refusal `code` is identical across runs
- **No INTERNAL_ERROR** across runs

> Text wording may vary. The system is judged on outcome class + refusal codes + citation presence.

---

## 3) Generated Pattern Panel (Non-Static Test Set)

The intent is to validate shape coverage without supporting a static list of questions.

### How it works
- Define a small set of **templates** per supported/unsupported shape.
- Expand templates by substituting:
  - dataset family (MBIE annual, MBIE quarterly, LAWA state, LAWA trend)
  - fuels (hydro/wind/solar/geothermal/coal/gas)
  - time windows (e.g., 2018–2023, since 2000, last 20 years)
  - regions/indicators (LAWA)
- Generate 50–150 prompts per run.
- Record results and evaluate against Gates A–C.

**The templates are stable; the expanded prompt set is variable.**
That keeps the test future-proof and avoids “supporting 10 magic questions forever.”

---

## 4) Refusal Codes (Phase 15 Contract)

### Required refusal codes (stable)
- `UNSUPPORTED_INTENT`
- `UNABLE_TO_PARSE`
- `CAPABILITY_UNSUPPORTED`
- `DATASET_MISMATCH`
- `MISSING_REQUIRED_FILTERS`

Phase 15 completion requires refusal codes to be meaningful, stable, and correct for the prompt shape.

---

## 5) Implementation Targets (Phase 15 hardening work)

This rubric assumes the following Phase 15 hardening targets are implemented:

1) **Citation validation softening**
- Validation must allow “evidence family” satisfaction (e.g., prefix/wildcard family matching) so equivalent citations do not cause INTERNAL_ERROR.

2) **Deterministic required-citation selection**
- When builders declare required citations, ordering/selection must be stable to avoid random pass/fail.

3) **Derived-analytics refusal triggers**
- Prompts requesting ranking/argmax/share/threshold must refuse as `CAPABILITY_UNSUPPORTED` unless Fact Packs explicitly include those derived results.

---

## 6) Phase 15 Closeout Artifacts

When Phase 15 is complete, the repo should include:
- `docs/phase15/phase15_exit_rubric.md` (this file)
- `docs/phase15/phase15_generated_panel_spec.md`
- A short entry in `decisions.md` recording:
  - family-prefix citation validation
  - determinism requirements for builders
  - derived-analytics refusal boundary (Phase 16)

