# Wai & Watts --- Capability-Driven Ask UI Spec (Remove Hardcoded Feel)

Generated: 2026-02-25

This spec eliminates the remaining "selective / hardcoded demo" feel by
making the Ask UI truly capability-driven. It focuses on the *Ask Page*
and supporting backend metadata so `/api/v1/capabilities` becomes the
single source of truth for: - capability labeling - dataset labeling -
example prompt templates - example parameter suggestions

No changes weaken guardrails or determinism.

------------------------------------------------------------------------

# 1. Problem Statement

Current Ask UI still feels selective/rigid because the frontend
hardcodes: - capability labels (`capabilityLabel()` mapping) - dataset
labels (`datasetLabel()` mapping) - default example token values
(`promptValueByToken`), biasing examples toward: - fuelType=hydro -
stateCategory=EXCELLENT - region=Auckland

This undermines the intent of `/api/v1/capabilities` as the UI discovery
contract.

------------------------------------------------------------------------

# 2. Goals

## Must achieve

-   Ask UI uses backend-provided `displayName`/`description` for
    capability chips.
-   Ask UI uses backend-provided dataset display names.
-   Example generation uses rotating / dynamic suggested values, not
    hydro/excellent defaults.
-   `/api/v1/capabilities` clearly communicates supported scope and
    examples.

## Must not do

-   No LLM computation or ad-hoc SQL.
-   No loosening of intent validation.
-   No new hardcoded allowlists in validation/parser.

------------------------------------------------------------------------

# 3. Frontend Changes (AskPage.tsx)

Target file: - `frontend/src/pages/AskPage.tsx` (or equivalent)

## 3.1 Remove hardcoded capability labels

Delete: - `capabilityLabel()` mapping by `questionType`

Replace chip label rendering with: - `capability.displayName`
(preferred) - fallback: `capability.description` - fallback:
`capability.questionType`

Acceptance: - UI does not map known question types to custom names
locally.

------------------------------------------------------------------------

## 3.2 Remove hardcoded dataset labels

Delete: - `datasetLabel()` mapping by datasetCode

Use dataset display name from capabilities payload: -
`datasetNames.get(datasetCode) ?? datasetCode`

Acceptance: - Ask UI does not contain a dictionary mapping datasetSource
→ display label.

------------------------------------------------------------------------

## 3.3 Replace hardcoded token defaults for examples

Delete: - `promptValueByToken` constant with fixed defaults
(hydro/excellent/Auckland)

Replace with one of the following approaches:

### Preferred: backend-provided suggested values (see Section 4)

-   `buildPromptFromTemplate()` selects token replacements using:
    -   `capabilities.data.suggestedValuesByToken[token]`
    -   rotating selection (day offset) to avoid static repetition

### Interim: frontend rotating pools (if backend change deferred)

Define local pools (not biased): - fuelType: \[wind, solar, hydro,
geothermal\] - fuelTypeB: \[hydro, wind, solar, coal\] - stateCategory:
\[EXCELLENT, GOOD, FAIR, POOR\] - region: \[Auckland, Canterbury, Otago,
Waikato\] - indicator: \[E. coli, Nitrate, Ammoniacal nitrogen\] -
trend: \[improving, declining, stable\]

Token selection should rotate deterministically: - use date offset to
pick items - do not always lead with the same category

Acceptance: - default generated examples do not always contain
hydro/excellent/Auckland. - across 4 visible examples, do not repeat
fuelType unless unavoidable.

------------------------------------------------------------------------

## 3.4 Example selection display

Current behavior: - combined templates + examples, then unique and
rotate 4

Keep structure but ensure: - If a capability is selected, examples shown
must be capability-specific. - If not selected, show a small global
rotating set. - Examples should be clickable and populate the input
textarea.

Acceptance: - "Try asking" always shows 3--4 valid questions without
requiring the user to guess.

------------------------------------------------------------------------

# 4. Backend Changes (Capabilities Payload Improvement)

## 4.1 Add suggested values for template tokens (Recommended)

Update `/api/v1/capabilities` response to include suggested values for
template tokens, e.g.:

``` json
{
  "suggestedValuesByToken": {
    "fuelType": ["wind","solar","hydro","geothermal"],
    "fuelTypeB": ["hydro","wind","solar","coal"],
    "stateCategory": ["EXCELLENT","GOOD","FAIR","POOR"],
    "region": ["Auckland","Canterbury","Otago","Waikato"],
    "indicator": ["E. coli","Nitrate","Ammoniacal nitrogen"],
    "trend": ["improving","declining","stable"]
  }
}
```

Source of truth: - If enumerations exist in backend domain models,
derive from those. - Otherwise supply a conservative curated list per
dataset.

Acceptance: - Ask UI can build varied example prompts without local
hardcoded defaults.

------------------------------------------------------------------------

# 5. Backend Metadata: QuestionTypeCatalog Descriptions

File: -
`backend/src/main/java/nz/waiwatts/explanations/service/QuestionTypeCatalog.java`

## 5.1 Ensure canonical intent IDs have polished descriptions

Add/confirm overrides for intents actually used in Phase 16: -
`fuel_generation_trend`: "Explain generation trends for a selected fuel
over time." - `water_quality_state_sites_trend`: "Track how site states
change over time."

Adjust existing descriptions to avoid "hydro vs geothermal" phrasing
unless presented as one of many examples.

Acceptance: - `/api/v1/capabilities` output reads product-like. - No
legacy/special-case wording dominates default descriptions.

------------------------------------------------------------------------

# 6. Acceptance Criteria (Definition of Done)

## 6.1 Capability-driven UI

-   No `capabilityLabel()` mapping exists in Ask UI.
-   No datasetSource → label mapping exists in Ask UI.
-   Chip labels use backend `displayName`/`description`.

## 6.2 Example prompts are dynamic and non-biased

-   Across the default 4 rotating examples, at least 2 distinct fuel
    types appear (when MBIE templates exist).
-   State category examples are not locked to EXCELLENT.
-   Region/indicator examples vary over time.

## 6.3 User discoverability improves

-   A first-time reviewer can click a chip and ask a supported question
    successfully on the first try.
-   If user asks unsupported question, refusal response shows guided
    alternatives (already implemented elsewhere).

## 6.4 Guardrails unchanged

-   Validation remains catalog-driven.
-   Citation requirements remain enforced.
-   No new hardcoded allowlists introduced in validation/parser.

------------------------------------------------------------------------

# 7. Notes for Implementation

-   If backend suggested token values are added, frontend should prefer
    them and fall back to conservative local pools if absent.
-   Keep deterministic rotation (date offset) so screenshots and demos
    are stable.
-   Keep technical IDs hidden/collapsed; present human-friendly names by
    default.

------------------------------------------------------------------------

# 8. PR Hygiene Recommendation (Optional)

If splitting PRs is still feasible: - PR 1: backend capabilities
payload + QuestionTypeCatalog descriptions - PR 2: AskPage UI refactor
(remove local label maps + token defaults)

If not feasible, ensure commits are logically separated within the
branch.
