# Phase 15 Hardening — Pseudo-Diff Patch Guidance (Builder-Ready)

This doc provides **file-agnostic, search-driven pseudo-diff guidance** for the three Phase 15 hardening items:
1) citation family matching
2) deterministic required citations
3) derived-analytics refusal triggers

It is written so Builder GPT can implement without guessing architecture.

---

## Patch 1 — Citation validation: family-prefix matching

### Problem
Valid explanations are rejected due to strict citation ID equality.
Result: `INTERNAL_ERROR` with message like “Generated explanation missing required citations.” (Observed in panel results.)

### Goal
Allow a required citation family like:
- `metric:lawa:excellent_sites_percentage:*`

to be satisfied by actual citations like:
- `metric:lawa:excellent_sites_percentage:auckland`

### Locate code (ripgrep hints)
Run:
- `rg -n "missing required citations|required citations|Citation" backend/src/main/java`
- `rg -n "validate.*citation|CitationValidation" backend/src/main/java`
- `rg -n "requiredCitations|required_citations|required citations" backend/src/main/java`

### Pseudo-diff (conceptual)
In the citation validator, replace strict containment:

```diff
- for (String required : requiredCitations) {
-   if (!actualCitationIds.contains(required)) { fail(required); }
- }
+ for (String required : requiredCitations) {
+   if (!isSatisfiedByAnyActual(required, actualCitationIds)) { fail(required); }
+ }
```

Add helper:

```diff
+ boolean isSatisfiedByAnyActual(String required, Set<String> actualIds) {
+   if (actualIds.contains(required)) return true;
+   // Family matching: required ends with ":*"
+   if (required.endsWith(":*")) {
+     String prefix = required.substring(0, required.length() - 1); // keep trailing ':'
+     for (String a : actualIds) {
+       if (a.startsWith(prefix)) return true;
+     }
+   }
+   return false;
+ }
```

### Tests
Add unit tests for:
- exact match passes
- family match passes
- wrong family fails

Add an integration test or harness regression:
- prompt “How do water quality trends compare across regions?”
- must not produce INTERNAL_ERROR solely due to citation mismatch

---

## Patch 2 — Deterministic required citation selection (builder-side)

### Problem
Required citations sometimes depend on iteration order or arbitrary selection, causing random pass/fail.

### Goal
Builders must output stable required citations for identical inputs:
- dedupe
- stable sort
- deterministic “representative selection” if needed

### Locate code (ripgrep hints)
- `rg -n "requiredCitations|RequiredCitations" backend/src/main/java`
- `rg -n "List<.*Citation|Set<.*Citation" backend/src/main/java`
- `rg -n "builder.*citation|fact pack.*citation" backend/src/main/java`

### Pseudo-diff (conceptual)
Where required citations are constructed:

```diff
- List<String> required = new ArrayList<>();
- for (Fact f : facts) { if (f.isPrimary()) required.add(f.id()); }
- return required;
+ return facts.stream()
+   .filter(Fact::isPrimary)
+   .map(Fact::id)
+   .distinct()
+   .sorted()
+   .toList();
```

If there is “pick N” logic, replace non-deterministic picks:

```diff
- return candidates.subList(0, N);
+ return candidates.stream().sorted().limit(N).toList();
```

### Tests
- Unit: builder produces identical required list across repeated calls
- Unit: shuffle input facts ordering => required list unchanged

---

## Patch 3 — Derived-analytics refusal triggers (Phase 16 boundary)

### Problem
Some prompts requesting Phase-16 analytics (“fastest”, “largest”, “which fuel grew most”, “share >80%”) are mapped into a supported trend question and answered with generic narrative.

### Goal
Detect derived-analytics language and refuse with `UNSUPPORTED_CAPABILITY` (or `UNSUPPORTED_INTENT` if you treat all derived analytics as “unsupported intent”).
Do **not** answer a different question “as if” answered.

### Where to implement
Prefer implementing this in the **intent parsing/validation layer**, not inside fact pack builders.
You want refusal before dataset selection / fact pack construction if possible.

### Locate code (ripgrep hints)
- `rg -n "IntentParser|parse.*intent|QuestionType" backend/src/main/java`
- `rg -n "RequestValidation|validate.*filters|UNSUPPORTED_CAPABILITY" backend/src/main/java`
- `rg -n "explanations/ask" backend/src/main/java`

### Detection (minimal heuristic)
In validation, add a derived-analytics detector over the raw question string (or parsed intent metadata if available):

Keywords / patterns:
- argmax/window: `fastest`, `biggest`, `largest`, `most`, `peak`, `max`, `minimum`
- ranking: `which`, `highest`, `lowest`, `top`
- share/threshold: `%`, `percent`, `share`, `exceed`, `over`, `above`, `threshold`

Pseudo:

```diff
+ if (looksLikeDerivedAnalytics(rawQuestion)) {
+   return refusal(UNSUPPORTED_CAPABILITY, "Derived analytics are Phase 16.");
+ }
```

### Tests
- “When did wind generation grow the fastest over any 3-year period?” => refusal UNSUPPORTED_CAPABILITY
- “Which fuel has grown the most since 2005?” => refusal UNSUPPORTED_CAPABILITY
- “When did renewables first exceed 80% of total generation?” => refusal UNSUPPORTED_CAPABILITY

---

## Verification checklist
After implementing patches:
1) Run harness 3 times; evaluator must pass Gates A–C
2) Ensure INTERNAL_ERROR disappears from panel outputs
3) Ensure derived-analytics prompts refuse rather than returning generic trend narratives

