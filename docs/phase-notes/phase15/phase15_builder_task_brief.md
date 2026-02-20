# Wai & Watts — Builder Task Brief (Phase 15 Hardening)

**Role:** Builder GPT (implementation)  
**Owner:** Human reviews + commits  
**Goal:** Implement Phase 15 hardening so the Ask feature is useful without devolving into a hardcoded query engine.

This brief is intentionally scoped: Phase 15 improves robustness and safety within existing shapes, not new analytics.

---

## 0) Definition of Done (Builder)

Implement the following with tests:

1) **Citation validation: family-prefix matching**
   - Reduce INTERNAL_ERROR caused by strict citation mismatch
2) **Deterministic required-citation selection**
   - Eliminate random pass/fail due to ordering/selection instability
3) **Derived-analytics refusal triggers**
   - Prevent “answering the wrong question” for Phase 16 prompts (argmax/ranking/share/threshold)

Then update docs:
- Add/refresh Phase 15 exit rubric
- Add Generated Pattern Panel spec
- Ensure Phase 15 maturity checklist points to rubric + generator

---

## 1) Patch: Citation Validation Softening (Family Prefix)

### Problem
Valid explanations are rejected because the LLM cites equivalent evidence with different concrete IDs.

### Target behavior
Required citation families like:
- `metric:lawa:excellent_sites_percentage:*`

should be satisfied by any actual citation:
- `metric:lawa:excellent_sites_percentage:auckland`

### Implementation guidance (non-binding)
- Add a matching layer:
  - exact match OR
  - requiredId ends with `:*` and actualId starts with requiredId prefix (without `*`)
  - optionally allow `:__ANY__` sentinel if present
- Keep existing strict mode available (feature flag optional, but avoid new surface area unless already present)

### Tests
- Unit test: required family satisfied by concrete id
- Unit test: unrelated id does not satisfy
- Integration: previously failing “regional trend comparison” should no longer produce INTERNAL_ERROR solely due to citation family mismatch

---

## 2) Patch: Deterministic Required Citation Selection

### Problem
Builders sometimes choose required citations from arbitrary ordering, creating random pass/fail.

### Target behavior
- Builders must produce stable ordered required-citation lists
- If selection is needed (e.g., choosing representative citations), selection must be deterministic:
  - sort by stable key
  - deterministic tie-breakers
  - avoid “first N” on non-deterministic collections

### Tests
- Unit test: builder returns same required list across runs (same fact pack input)
- Property-like test: shuffle input ordering, output required list remains identical

---

## 3) Patch: Derived-Analytics Refusal Triggers (Phase 16 Boundary)

### Problem
Some prompts requesting ranking/argmax/share/threshold are mapped into a supported trend question and answered with generic narrative.

### Target behavior
If the user asks for a derived analytic that is not explicitly present in Fact Packs:
- refuse with `CAPABILITY_UNSUPPORTED`
- do not answer a different question “as if” answered

### Examples to detect
- "fastest", "biggest increase", "largest drop"
- "which fuel", "highest", "most", "rank"
- "% of total", "share", "exceed", "threshold", "> 80%"

### Tests
- Unit test: intent validation rejects derived-analytics prompts as CAPABILITY_UNSUPPORTED
- End-to-end: those prompts do not return a trend answer

---

## 4) Add Generated Pattern Panel Harness

### Goal
Replace static “10 questions must pass” with a template-based generator.

### Minimal deliverable
- A script or test utility that:
  - generates prompts from templates + substitutions
  - labels each prompt as supported/unsupported
  - runs `/api/v1/explanations/ask` (or equivalent)
  - writes JSONL output + summary
- Run 3 times; produce determinism report

### Tests
- Smoke test: harness runs and produces output file
- Evaluation test: summary evaluator enforces Phase 15 gates (can be a CI-optional test if runtime/LLM cost is a concern)

---

## 5) Documentation updates (drop-in)

- `docs/phase15/phase15_exit_rubric.md`
- `docs/phase15/phase15_generated_panel_spec.md`
- Update Phase 15 checklist to reference these docs as authoritative

---

## 6) Non-goals (Builder must refuse scope creep)

- Adding new analytics computations (argmax, shares, rankings)
- Expanding question type enum
- Changing Fact Pack contract
- Introducing new infra (queues, schedulers, caches)

If any of those are required, escalate “Needs Staff decision.”

