# Phase 15 Architectural Review

This document is an architectural audit log.
It is observational and not executable.
Remediation is governed exclusively by:
docs/phase-notes/phase15/phase15_architectural_remediation_plan.md


## Slice A — Ask Pipeline

### Batch A1 — Controller + Service

#### Critical
### Finding A1-01
Category: Critical
Location: backend/src/main/java/nz/waiwatts/explanations/service/ExplanationServiceImpl.java:328 — Class `ExplanationServiceImpl`, method `selectFactPackBuilder(ExplanationRequest)`
Description:
builder selection uses `findFirst()` over injected `List<FactPackBuilder>`; if multiple builders match `canHandle=true`, behavior can depend on bean order, creating determinism risk.
Status: Unresolved

### Finding A1-02
Category: Critical
Location: backend/src/main/java/nz/waiwatts/explanations/api/ExplanationController.java:251 — Class `ExplanationController`, method `askQuestion(Map<String, String>)`
Description:
top-level `catch (Exception)` rethrows, so predictable runtime failures can surface as HTTP 500 instead of ask refusal envelope.
Status: Unresolved

#### Improvement
### Finding A1-03
Category: Improvement
Location: backend/src/main/java/nz/waiwatts/explanations/api/ExplanationController.java:92 — Class `ExplanationController`, method `askQuestion(Map<String, String>)`
Description:
controller owns parse + dataset selection + validation + explanation/refusal mapping, increasing boundary drift risk.
Status: Unresolved

### Finding A1-04
Category: Improvement
Location: backend/src/main/java/nz/waiwatts/explanations/service/ExplanationServiceImpl.java:290 — Class `ExplanationServiceImpl`, method `validateTimeRangeFilters(Map<String, Object>)`
Description:
logs auto-swap for `startYear > endYear` but does not persist normalized values.
Status: Unresolved

### Finding A1-05
Category: Improvement
Location: backend/src/main/java/nz/waiwatts/explanations/api/ExplanationController.java:315 — Class `ExplanationController`, method `mapExplanationRefusalCode(String)`
Description:
unknown reasons default to `UNSUPPORTED_CAPABILITY`, which can mask internal defects.
Status: Unresolved

#### Nice-to-have
### Finding A1-06
Category: Nice-to-have
Location: backend/src/main/java/nz/waiwatts/explanations/api/ExplanationController.java:94 — Class `ExplanationController`, method `askQuestion(Map<String, String>)`
Description:
INFO logs full request body; question-length logging may be sufficient.
Status: Unresolved

### Finding A1-07
Category: Nice-to-have
Location: backend/src/main/java/nz/waiwatts/explanations/service/ExplanationServiceImpl.java:250 — Class `ExplanationServiceImpl`, method `validateRequest(ExplanationRequest)`
Description:
validation mutates request (`setDatasetSource`), which can reduce test clarity.
Status: Unresolved

#### Questions
### Finding A1-08
Category: Question
Location: backend/src/main/java/nz/waiwatts/explanations/service/ExplanationServiceImpl.java:328 — Class `ExplanationServiceImpl`, method `selectFactPackBuilder(ExplanationRequest)`
Description:
is there a test/invariant ensuring exactly one builder handles each `(questionType, datasetSource)`?
Status: Unresolved

### Finding A1-09
Category: Question
Location: backend/src/main/java/nz/waiwatts/explanations/api/ExplanationController.java:251 — Class `ExplanationController`, method `askQuestion(Map<String, String>)`
Description:
is there global exception handling that guarantees deterministic ask refusal payloads vs generic 500?
Status: Unresolved

### Finding A1-10
Category: Question
Location: backend/src/main/java/nz/waiwatts/explanations/service/ExplanationServiceImpl.java:180 — Class `ExplanationServiceImpl`, method `validateCitations(Explanation, FactPack)`
Description:
is uncited numeric-claim enforcement handled elsewhere, or is required-citation presence the complete enforcement?
Status: Unresolved

### Batch A2 — Intent Parsing + Dataset Selection + Validation

#### Critical
### Finding A2-01
Category: Critical
Location: backend/src/main/java/nz/waiwatts/explanations/service/RequestValidationService.java:123 — Class `RequestValidationService`, method `validateFilters(ExplanationRequest)`
Description:
`startYear`/`endYear` are cast to `Integer` before type-checking (`(Integer) filters.get(...)`). Non-integer values can throw `ClassCastException`, which can bubble to `/ask` as HTTP 500 instead of a refusal.
Status: Unresolved

### Finding A2-02
Category: Critical
Location: backend/src/main/java/nz/waiwatts/explanations/service/DatasetSelectionService.java:109 — Class `DatasetSelectionService`, method `selectDataset(String, ExplanationRequest)`
Description:
when both MBIE datasets are valid, selection is based on LLM candidate order (`for` loop over ranked candidates) with no deterministic tie-breaker, creating run-to-run dataset/fact-pack/citation variance.
Status: Unresolved

#### Improvement
### Finding A2-03
Category: Improvement
Location: backend/src/main/java/nz/waiwatts/explanations/service/DatasetSelectionService.java:84 — Class `DatasetSelectionService`, method `selectDataset(String, ExplanationRequest)`
Description:
refusal code taxonomy is inconsistent (`UNSUPPORTED_CAPABILITY` and `CAPABILITY_UNSUPPORTED` both used in same class), which increases contract drift risk in `/ask` responses.
Status: Unresolved

### Finding A2-04
Category: Improvement
Location: backend/src/main/java/nz/waiwatts/explanations/service/IntentParserServiceImpl.java:31 — Class `IntentParserServiceImpl`, method `parseQuestion(String)`
Description:
fallback parser is instantiated directly (`new HardcodedDemoIntentParser()`), reducing seam quality for unit testing fallback behavior.
Status: Unresolved

### Finding A2-05
Category: Improvement
Location: backend/src/main/java/nz/waiwatts/explanations/service/RequestValidationService.java:23 — Class `RequestValidationService`, method `validateRequest(ExplanationRequest)`
Description:
supported question/dataset constants are duplicated here (also defined in parser/catalog contexts), creating silent drift risk across parse/selection/validation phases.
Status: Unresolved

#### Nice-to-have
### Finding A2-06
Category: Nice-to-have
Location: backend/src/main/java/nz/waiwatts/explanations/parser/OpenAiIntentParser.java:196 — Class `OpenAiIntentParser`, method `buildSchema()`
Description:
schema requires all filter fields (nullable), which is valid but forces null-heavy payloads and extra normalization churn.
Status: Unresolved

### Finding A2-07
Category: Nice-to-have
Location: backend/src/main/java/nz/waiwatts/explanations/service/IntentParserServiceImpl.java:45 — Class `IntentParserServiceImpl`, method `parseQuestion(String)`
Description:
logs full natural-language question at INFO; consider minimizing logged user text for steady-state operation.
Status: Unresolved

#### Questions
### Finding A2-08
Category: Question
Location: backend/src/main/java/nz/waiwatts/explanations/service/DatasetSelectionService.java:109 — Class `DatasetSelectionService`, method `selectDataset(String, ExplanationRequest)`
Description:
what is the intended deterministic rule when both `mbie.generation.annual` and `mbie.generation.quarterly` are valid?
Status: Unresolved

### Finding A2-09
Category: Question
Location: backend/src/main/java/nz/waiwatts/explanations/service/DatasetSelectionService.java:84 — Class `DatasetSelectionService`, method `selectDataset(String, ExplanationRequest)`
Description:
which refusal code spelling is canonical for unsupported capability (`UNSUPPORTED_CAPABILITY` vs `CAPABILITY_UNSUPPORTED`)?
Status: Unresolved

### Finding A2-10
Category: Question
Location: backend/src/main/java/nz/waiwatts/explanations/service/IntentParserServiceImpl.java:103 — Class `IntentParserServiceImpl`, method `parseQuestion(String)`
Description:
should LLM failure continue to permit demo-parser success paths in non-demo environments, or should it always refuse for strict operational transparency?
Status: Unresolved

### Batch A3 — Provider + Citation Mapping + MBIE Annual Fact Pack Builder

#### Critical
### Finding A3-01
Category: Critical
Location: backend/src/main/java/nz/waiwatts/explanations/provider/StubExplanationProvider.java:303 — Class `StubExplanationProvider`, method `generateWaterQualityOverviewExplanation(FactPack)`
Description:
explanation text can include numeric metric values (excellent/poor percentages) while returned citations are classification IDs only (`class:*`), creating uncited numeric-claim risk under the citation contract.
Status: Unresolved

### Finding A3-02
Category: Critical
Location: backend/src/main/java/nz/waiwatts/explanations/builder/MbieGenerationAnnualFactPackBuilder.java:103 — Class `MbieGenerationAnnualFactPackBuilder`, method `getRecordsForRequest(ExplanationRequest)`
Description:
uses `repository.findAll()` across all releases without release scoping, so facts may aggregate multiple releases and produce non-canonical totals for ask responses.
Status: Unresolved

#### Improvement
### Finding A3-03
Category: Improvement
Location: backend/src/main/java/nz/waiwatts/explanations/service/CitationMapper.java:16 — Class `CitationMapper`, method `map(List<String>, String)`
Description:
preserves provider citation order and duplicates as-is; response citation list is therefore not stabilized (sort/dedupe), which is a determinism risk for `/ask` output.
Status: Unresolved

### Finding A3-04
Category: Improvement
Location: backend/src/main/java/nz/waiwatts/explanations/provider/OpenAiExplanationProvider.java:97 — Class `OpenAiExplanationProvider`, method `parseExplanation(String)`
Description:
accepts any parseable `Explanation` shape without enforcing non-empty `explanationText` when `isRefusal=false`; malformed-but-parseable provider payloads can degrade response quality.
Status: Unresolved

### Finding A3-05
Category: Improvement
Location: backend/src/main/java/nz/waiwatts/explanations/builder/MbieGenerationAnnualFactPackBuilder.java:397 — Class `MbieGenerationAnnualFactPackBuilder`, method `setGuardrails(FactPack, ExplanationRequest)`
Description:
required-citation derivation differs by question type and can be broader than claims actually needed (`Integer.MAX_VALUE` paths), increasing over-constrained citation validation risk.
Status: Unresolved

#### Nice-to-have
### Finding A3-06
Category: Nice-to-have
Location: backend/src/main/java/nz/waiwatts/explanations/provider/OpenAiExplanationProvider.java:62 — Class `OpenAiExplanationProvider`, method `validateCitations(Explanation, FactPack)`
Description:
citation matcher logic duplicates service logic (`ExplanationServiceImpl`) and can drift over time.
Status: Unresolved

### Finding A3-07
Category: Nice-to-have
Location: backend/src/main/java/nz/waiwatts/explanations/builder/FactPackBuilder.java:37 — Interface `FactPackBuilder`, method `getSupportedDatasetSourceCode()`
Description:
currently not used in selection path, reducing clarity of the intended selection contract.
Status: Unresolved

#### Questions
### Finding A3-08
Category: Question
Location: backend/src/main/java/nz/waiwatts/explanations/builder/MbieGenerationAnnualFactPackBuilder.java:103 — Class `MbieGenerationAnnualFactPackBuilder`, method `getRecordsForRequest(ExplanationRequest)`
Description:
should ask explanations use only one canonical release (latest or selected lineage), instead of aggregating all persisted releases?
Status: Unresolved

### Finding A3-09
Category: Question
Location: backend/src/main/java/nz/waiwatts/explanations/service/CitationMapper.java:16 — Class `CitationMapper`, method `map(List<String>, String)`
Description:
do you want citation order in `/ask` responses to be deterministic (e.g., sorted IDs) for repeatability/tests?
Status: Unresolved

### Finding A3-10
Category: Question
Location: backend/src/main/java/nz/waiwatts/explanations/provider/StubExplanationProvider.java:303 — Class `StubExplanationProvider`, method `generateWaterQualityOverviewExplanation(FactPack)`
Description:
is it acceptable for stub mode to emit numeric claims without metric citations, or should stub enforce the same strict citation granularity as production mode?
Status: Unresolved

### Batch A4 — Remaining Fact Pack Builders + Provider Interface + Explanation DTO

#### Critical
### Finding A4-01
Category: Critical
Location: backend/src/main/java/nz/waiwatts/explanations/builder/LawaStateMultiYearFactPackBuilder.java:153 — Class `LawaStateMultiYearFactPackBuilder`, method `getRecordsForRequest(ExplanationRequest)`
Description:
indicator filter is effectively bypassed when `region` is not provided (`fIndicator == null || fRegion == null || indicatorMatches`). This allows broader data than requested into Fact Pack.
Status: Unresolved

### Finding A4-02
Category: Critical
Location: backend/src/main/java/nz/waiwatts/explanations/builder/LawaTrendMultiYearFactPackBuilder.java:160 — Class `LawaTrendMultiYearFactPackBuilder`, method `getRecordsForRequest(ExplanationRequest)`
Description:
same predicate pattern bypasses indicator-only filtering unless region is also provided, causing incorrect dataset subset selection.
Status: Unresolved

### Finding A4-03
Category: Critical
Location: backend/src/main/java/nz/waiwatts/explanations/builder/MbieGenerationQuarterlyFactPackBuilder.java:113 — Class `MbieGenerationQuarterlyFactPackBuilder`, method `getRecordsForRequest(ExplanationRequest)`
Description:
reads `repository.findAll()` without canonical release scoping; ask answers may aggregate across historical releases instead of a single deterministic release.
Status: Unresolved

### Finding A4-04
Category: Critical
Location: backend/src/main/java/nz/waiwatts/explanations/builder/LawaStateMultiYearFactPackBuilder.java:115 — Class `LawaStateMultiYearFactPackBuilder`, method `getRecordsForRequest(ExplanationRequest)`
Description:
same all-releases aggregation risk for LAWA state ask answers.
Status: Unresolved

### Finding A4-05
Category: Critical
Location: backend/src/main/java/nz/waiwatts/explanations/builder/LawaTrendMultiYearFactPackBuilder.java:115 — Class `LawaTrendMultiYearFactPackBuilder`, method `getRecordsForRequest(ExplanationRequest)`
Description:
same all-releases aggregation risk for LAWA trend ask answers.
Status: Unresolved

#### Improvement
### Finding A4-06
Category: Improvement
Location: backend/src/main/java/nz/waiwatts/explanations/builder/LawaStateMultiYearFactPackBuilder.java:358 — Class `LawaStateMultiYearFactPackBuilder`, method `buildRegionalWaterQualityFacts(FactPack, List<LawaStateMultiYearRecord>)`
Description:
regional percentage denominator uses record counts via `Collectors.counting()` over `lawaSiteId`, not distinct-site counts, which can bias percentages when a site has multiple rows.
Status: Unresolved

### Finding A4-07
Category: Improvement
Location: backend/src/main/java/nz/waiwatts/explanations/builder/LawaTrendMultiYearFactPackBuilder.java:387 — Class `LawaTrendMultiYearFactPackBuilder`, method `buildRegionalTrendComparisonFacts(FactPack, List<LawaTrendMultiYearRecord>)`
Description:
same non-distinct counting risk for regional percentage metrics.
Status: Unresolved

### Finding A4-08
Category: Improvement
Location: backend/src/main/java/nz/waiwatts/explanations/builder/LawaStateMultiYearFactPackBuilder.java:538 — Class `LawaStateMultiYearFactPackBuilder`, method `buildRegionalWaterQualityRequiredCitations(FactPack)`
Description:
required citations are all family-level wildcards across selected regions (`:*`) with `Integer.MAX_VALUE`; this is deterministic but may be stricter than necessary for the exact generated claims.
Status: Unresolved

### Finding A4-09
Category: Improvement
Location: backend/src/main/java/nz/waiwatts/explanations/builder/LawaTrendMultiYearFactPackBuilder.java:567 — Class `LawaTrendMultiYearFactPackBuilder`, method `buildRegionalTrendRequiredCitations(FactPack)`
Description:
same broad required-family construction pattern.
Status: Unresolved

#### Nice-to-have
### Finding A4-10
Category: Nice-to-have
Location: backend/src/main/java/nz/waiwatts/explanations/provider/ExplanationProvider.java:37 — Interface `ExplanationProvider`, method `validateCitations(Explanation, FactPack)`
Description:
validation responsibility is duplicated with service-layer validation; interface contract may imply provider-owned enforcement even though service is authoritative.
Status: Unresolved

### Finding A4-11
Category: Nice-to-have
Location: backend/src/main/java/nz/waiwatts/explanations/dto/Explanation.java:35 — Class `Explanation`, method `refusal(String)`
Description:
hardcoded user-facing refusal text is generic and not taxonomy-aware; reason is preserved separately, but this can blur refusal UX consistency.
Status: Unresolved

#### Questions
### Finding A4-12
Category: Question
Location: backend/src/main/java/nz/waiwatts/explanations/builder/LawaStateMultiYearFactPackBuilder.java:153 — Class `LawaStateMultiYearFactPackBuilder`, method `getRecordsForRequest(ExplanationRequest)`
Description:
was indicator filtering intentionally coupled to region for this phase, or is this an unintended predicate bug?
Status: Unresolved

### Finding A4-13
Category: Question
Location: backend/src/main/java/nz/waiwatts/explanations/builder/LawaTrendMultiYearFactPackBuilder.java:160 — Class `LawaTrendMultiYearFactPackBuilder`, method `getRecordsForRequest(ExplanationRequest)`
Description:
same question for trend builder.
Status: Unresolved

### Finding A4-14
Category: Question
Location: backend/src/main/java/nz/waiwatts/explanations/builder/MbieGenerationQuarterlyFactPackBuilder.java:113 — Class `MbieGenerationQuarterlyFactPackBuilder`, method `getRecordsForRequest(ExplanationRequest)`
Description:
should Fact Pack construction be pinned to a canonical release ID/content hash for ask determinism?
Status: Unresolved

### Batch A5 — Explanation Tests + OpenAI Client

#### Critical
### Finding A5-01
Category: Critical
Location: backend/src/test/java/nz/waiwatts/explanations/api/NaturalLanguageEndpointIntegrationTest.java:44 — Class `NaturalLanguageEndpointIntegrationTest`, method `testSuccessfulQuestionProcessing()` (and class pattern)
Description:
the `/ask` "integration" suite mocks `ExplanationService`, `IntentParserService`, `DatasetSelectionService`, and `RequestValidationService`, so it does not exercise the real ask pipeline path end-to-end (parser -> selection -> validation -> fact pack -> provider -> citation validation).
Status: Unresolved

### Finding A5-02
Category: Critical
Location: backend/src/test/java/nz/waiwatts/explanations/service/ExplanationServiceImplEdgeCaseTest.java:172 — Class `ExplanationServiceImplEdgeCaseTest`, method `testBuilderThrowsException()` (also `testProviderThrowsException()`)
Description:
tests assert exception propagation as expected behavior, reinforcing runtime 500 behavior instead of deterministic refusal mapping for user-path failures.
Status: Unresolved

#### Improvement
### Finding A5-03
Category: Improvement
Location: backend/src/test/java/nz/waiwatts/explanations/service/DatasetSelectionServiceTest.java:92 — Class `DatasetSelectionServiceTest`, method `refusesInvalidCandidate()`
Description:
expects `CAPABILITY_UNSUPPORTED`, while adjacent test expects `UNSUPPORTED_CAPABILITY` (`:130`), reflecting and normalizing refusal-code inconsistency instead of pinning a single canonical code.
Status: Unresolved

### Finding A5-04
Category: Improvement
Location: backend/src/test/java/nz/waiwatts/explanations/api/ExplanationControllerRefusalIntegrationTest.java:129 — Class `ExplanationControllerRefusalIntegrationTest`, method `testHealthCheck()`
Description:
hard-codes `phase=11`; this can drift from active phase and weakens portfolio credibility signals.
Status: Unresolved

### Finding A5-05
Category: Improvement
Location: backend/src/main/java/nz/waiwatts/explanations/provider/OpenAiResponseClient.java:131 — Class `OpenAiResponseClient`, method `extractOutputText(String)`
Description:
assumes `contentItem.get("text")` exists when type is `output_text`/`text`; missing field would raise runtime error and escape null-safe handling.
Status: Unresolved

#### Nice-to-have
### Finding A5-06
Category: Nice-to-have
Location: backend/src/test/java/nz/waiwatts/explanations/api/NaturalLanguageEndpointIntegrationTest.java:24 — Class `NaturalLanguageEndpointIntegrationTest`, class scope
Description:
imports `org.junit.jupiter.api.Assertions.*` but uses only MockMvc assertions; minor cleanup.
Status: Unresolved

### Finding A5-07
Category: Nice-to-have
Location: backend/src/main/java/nz/waiwatts/explanations/provider/OpenAiResponseClient.java:32 — Class `OpenAiResponseClient`, methods `createResponse(...)` and `createResponseWithSchema(...)`
Description:
duplicated request/response plumbing could be consolidated to reduce drift.
Status: Unresolved

#### Questions
### Finding A5-08
Category: Question
Location: backend/src/test/java/nz/waiwatts/explanations/api/NaturalLanguageEndpointIntegrationTest.java:44 — Class `NaturalLanguageEndpointIntegrationTest`
Description:
do you want one non-mocked "true ask pipeline" integration test in-slice to protect boundary regressions?
Status: Unresolved

### Finding A5-09
Category: Question
Location: backend/src/test/java/nz/waiwatts/explanations/service/ExplanationServiceImplEdgeCaseTest.java:172 — Class `ExplanationServiceImplEdgeCaseTest`
Description:
should builder/provider runtime failures continue to propagate by design, or should service return deterministic refusals for this path?
Status: Unresolved

### Finding A5-10
Category: Question
Location: backend/src/main/java/nz/waiwatts/explanations/provider/OpenAiResponseClient.java:46 — Class `OpenAiResponseClient`, method `createResponse(...)`
Description:
should malformed base URL / request construction errors be normalized to `null` responses like IO/HTTP failures?
Status: Unresolved

### Batch A6 — Dataset Catalog + Unsupported Intent Detection + Ask/FactPack DTOs + IntentParser tests

#### Critical
### Finding A6-01
Category: Critical
Location: backend/src/main/java/nz/waiwatts/explanations/dto/FactPack.java:25 — Class `FactPack`, constructor `FactPack()`
Description:
`generatedAtUtc` is set with `OffsetDateTime.now()`, introducing non-deterministic timestamps in Fact Pack payloads and tests unless consistently ignored.
Status: Unresolved

#### Improvement
### Finding A6-02
Category: Improvement
Location: backend/src/main/java/nz/waiwatts/explanations/parser/UnsupportedIntentDetector.java:27 — Class `UnsupportedIntentDetector`, method `isUnsupported(String)`
Description:
predictive pattern `\bwill\b` is overly broad and can classify benign explanatory phrasing as unsupported, increasing false-refusal risk.
Status: Unresolved

### Finding A6-03
Category: Improvement
Location: backend/src/main/java/nz/waiwatts/explanations/dataset/DatasetCatalog.java:12 — Class `DatasetCatalog`, method `getDatasets()`
Description:
catalog values duplicate request-validation constants (question types/filter sets), creating drift risk between selection and validation logic.
Status: Unresolved

### Finding A6-04
Category: Improvement
Location: backend/src/main/java/nz/waiwatts/explanations/dto/AskResult.java:15 — Class `AskResult`, field `dataSummary`
Description:
field exists in response envelope but is not populated in reviewed ask flow, which can create contract ambiguity for clients.
Status: Unresolved

#### Nice-to-have
### Finding A6-05
Category: Nice-to-have
Location: backend/src/test/java/nz/waiwatts/explanations/service/IntentParserServiceImplTest.java:18 — Class `IntentParserServiceImplTest`
Description:
current suite covers core happy/refusal paths but does not assert the normalization branch (`fuelTypeB` -> `fuel_type_comparison`) or LLM-exception fallback path.
Status: Unresolved

### Finding A6-06
Category: Nice-to-have
Location: backend/src/main/java/nz/waiwatts/explanations/dataset/DatasetCatalog.java:64 — Class `DatasetCatalog`, method `getDatasets()`
Description:
returns internal list directly (immutable now), but explicit defensive copy return would make contract intent clearer.
Status: Unresolved

#### Questions
### Finding A6-07
Category: Question
Location: backend/src/main/java/nz/waiwatts/explanations/dto/FactPack.java:25 — Class `FactPack`, constructor `FactPack()`
Description:
do you want `generatedAtUtc` included in outputs, or should this be optional/externally set for deterministic comparisons?
Status: Unresolved

### Finding A6-08
Category: Question
Location: backend/src/main/java/nz/waiwatts/explanations/parser/UnsupportedIntentDetector.java:27 — Class `UnsupportedIntentDetector`, method `isUnsupported(String)`
Description:
is the broad `will` refusal intended, or should predictive detection rely on tighter phrases to reduce false positives?
Status: Unresolved

### Finding A6-09
Category: Question
Location: backend/src/main/java/nz/waiwatts/explanations/dto/AskResult.java:15 — Class `AskResult`
Description:
should `dataSummary` be populated by `/ask`, or removed from the envelope until used?
Status: Unresolved

### Batch A7 — Explanations Config + Service Interfaces

#### Critical
### Finding A7-01
Category: Critical
Location: backend/src/main/java/nz/waiwatts/explanations/config/ExplanationConfig.java:21 — Class `ExplanationConfig`, methods `mbieGenerationAnnualFactPackBuilder(...)`/`...Quarterly...`/`...Lawa...`
Description:
all beans are declared under shared type `FactPackBuilder` without explicit ordering/qualification contract; combined with first-match selection in service, this leaves deterministic builder resolution dependent on container order/invariants outside config.
Status: Unresolved

#### Improvement
### Finding A7-02
Category: Improvement
Location: backend/src/main/java/nz/waiwatts/explanations/config/LlmProviderConfig.java:39 — Class `LlmProviderConfig`, method `explanationProvider(...)`
Description:
falls back to `StubExplanationProvider` whenever config is incomplete/unknown. This is pragmatic but can hide misconfiguration in environments expecting strict LLM behavior.
Status: Unresolved

### Finding A7-03
Category: Improvement
Location: backend/src/main/java/nz/waiwatts/explanations/config/LlmProviderConfig.java:59 — Class `LlmProviderConfig`, method `intentParser(...)`
Description:
similar fallback to `HardcodedDemoIntentParser` on config issues; this can produce demo-style behavior in non-demo runtime if configuration drifts.
Status: Unresolved

### Finding A7-04
Category: Improvement
Location: backend/src/main/java/nz/waiwatts/explanations/config/LlmProperties.java:70 — Class `LlmProperties`, method `isConfigured()`
Description:
all-or-nothing check includes `baseUrl`; any missing field silently pushes stub/demo fallback rather than explicit startup failure path.
Status: Unresolved

#### Nice-to-have
### Finding A7-05
Category: Nice-to-have
Location: backend/src/main/java/nz/waiwatts/explanations/service/ExplanationService.java:35 — Interface `ExplanationService`, method `buildFactPack(ExplanationRequest)`
Description:
returns `Object`, which weakens compile-time contract clarity for debug endpoint behavior.
Status: Unresolved

### Finding A7-06
Category: Nice-to-have
Location: backend/src/main/java/nz/waiwatts/explanations/service/IntentParserService.java:21 — Interface `IntentParserService`, method `parseQuestion(String)`
Description:
no explicit contract note on fallback semantics (`DEMO`, `LLM_FALLBACK_DEMO`) despite those being observable via response debug fields.
Status: Unresolved

#### Questions
### Finding A7-07
Category: Question
Location: backend/src/main/java/nz/waiwatts/explanations/config/LlmProviderConfig.java:39 — Class `LlmProviderConfig`, method `explanationProvider(...)`
Description:
should non-test/non-demo profiles fail fast on invalid LLM config instead of silently falling back to stub?
Status: Unresolved

### Finding A7-08
Category: Question
Location: backend/src/main/java/nz/waiwatts/explanations/config/LlmProviderConfig.java:59 — Class `LlmProviderConfig`, method `intentParser(...)`
Description:
should parser fallback policy be environment-gated (demo only) to avoid accidental production/demo behavior overlap?
Status: Unresolved

### Finding A7-09
Category: Question
Location: backend/src/main/java/nz/waiwatts/explanations/config/ExplanationConfig.java:21 — Class `ExplanationConfig`
Description:
do you want explicit bean ordering/uniqueness assertions to enforce deterministic builder selection at wiring time?
Status: Unresolved

## Slice B — Backend Dataset APIs (Read / Browse Endpoints)

### Batch B1 — LAWA + MBIE Controllers

#### Critical
### Finding B1-01
Category: Critical
Location: backend/src/main/java/nz/waiwatts/api/lawa/LawaStateMultiYearController.java:28 — Class `LawaStateMultiYearController`, method `getStateMultiYear(...)`
Description:
endpoint returns unpaginated full result list (`List<LawaStateMultiYearRecordDto>`) with no `limit/offset/page` guard, creating unbounded-response risk.
Status: Unresolved

### Finding B1-02
Category: Critical
Location: backend/src/main/java/nz/waiwatts/api/lawa/LawaTrendMultiYearController.java:28 — Class `LawaTrendMultiYearController`, method `getTrendMultiYear(...)`
Description:
same unpaginated full-list behavior.
Status: Unresolved

### Finding B1-03
Category: Critical
Location: backend/src/main/java/nz/waiwatts/api/mbie/MbieGenerationAnnualController.java:35 — Class `MbieGenerationAnnualController`, method `getGenerationAnnual(...)`
Description:
same unpaginated full-list behavior.
Status: Unresolved

### Finding B1-04
Category: Critical
Location: backend/src/main/java/nz/waiwatts/api/mbie/MbieGenerationQuarterlyController.java:35 — Class `MbieGenerationQuarterlyController`, method `getQuarterly(...)`
Description:
same unpaginated full-list behavior.
Status: Unresolved

#### Improvement
### Finding B1-05
Category: Improvement
Location: backend/src/main/java/nz/waiwatts/api/lawa/LawaStateMultiYearController.java:29 — Class `LawaStateMultiYearController`, method `getStateMultiYear(...)`
Description:
response type is `ResponseEntity<?>` and returns raw list on success vs `{ "error": ... }` map on validation failure; weakly typed and inconsistent envelope semantics for clients.
Status: Unresolved

### Finding B1-06
Category: Improvement
Location: backend/src/main/java/nz/waiwatts/api/lawa/LawaTrendMultiYearController.java:29 — Class `LawaTrendMultiYearController`, method `getTrendMultiYear(...)`
Description:
same weakly typed response pattern.
Status: Unresolved

### Finding B1-07
Category: Improvement
Location: backend/src/main/java/nz/waiwatts/api/mbie/MbieGenerationAnnualController.java:36 — Class `MbieGenerationAnnualController`, method `getGenerationAnnual(...)`
Description:
same weakly typed response pattern.
Status: Unresolved

### Finding B1-08
Category: Improvement
Location: backend/src/main/java/nz/waiwatts/api/mbie/MbieGenerationQuarterlyController.java:36 — Class `MbieGenerationQuarterlyController`, method `getQuarterly(...)`
Description:
same weakly typed response pattern.
Status: Unresolved

### Finding B1-09
Category: Improvement
Location: backend/src/main/java/nz/waiwatts/api/lawa/LawaStateMultiYearController.java:29 — Class `LawaStateMultiYearController`, method `getStateMultiYear(...)`
Description:
no dataset release/version parameters are exposed at controller level, making release semantics non-obvious for browse consumers.
Status: Unresolved

### Finding B1-10
Category: Improvement
Location: backend/src/main/java/nz/waiwatts/api/lawa/LawaTrendMultiYearController.java:29 — Class `LawaTrendMultiYearController`, method `getTrendMultiYear(...)`
Description:
same release/version visibility gap.
Status: Unresolved

### Finding B1-11
Category: Improvement
Location: backend/src/main/java/nz/waiwatts/api/mbie/MbieGenerationAnnualController.java:36 — Class `MbieGenerationAnnualController`, method `getGenerationAnnual(...)`
Description:
same release/version visibility gap.
Status: Unresolved

### Finding B1-12
Category: Improvement
Location: backend/src/main/java/nz/waiwatts/api/mbie/MbieGenerationQuarterlyController.java:36 — Class `MbieGenerationQuarterlyController`, method `getQuarterly(...)`
Description:
same release/version visibility gap.
Status: Unresolved

#### Nice-to-have
### Finding B1-13
Category: Nice-to-have
Location: backend/src/main/java/nz/waiwatts/api/lawa/LawaStateMultiYearController.java:35 — Class `LawaStateMultiYearController`, method `getStateMultiYear(...)`
Description:
controller validates only `fromYear <= toYear`; no local normalization/validation for blank `indicator`/`region` inputs before passing to service.
Status: Unresolved

### Finding B1-14
Category: Nice-to-have
Location: backend/src/main/java/nz/waiwatts/api/lawa/LawaTrendMultiYearController.java:35 — Class `LawaTrendMultiYearController`, method `getTrendMultiYear(...)`
Description:
same limited validation shape.
Status: Unresolved

#### Questions
### Finding B1-15
Category: Question
Location: backend/src/main/java/nz/waiwatts/api/lawa/LawaStateMultiYearController.java:28 — Class `LawaStateMultiYearController`, method `getStateMultiYear(...)`
Description:
is unpaginated full-list return intentional for current dataset sizes, or should browse endpoints advertise paging semantics in this slice?
Status: Unresolved

### Finding B1-16
Category: Question
Location: backend/src/main/java/nz/waiwatts/api/mbie/MbieGenerationAnnualController.java:35 — Class `MbieGenerationAnnualController`, method `getGenerationAnnual(...)`
Description:
should dataset release/version selection be explicit query params, or intentionally internal/defaulted at service layer?
Status: Unresolved

### Finding B1-17
Category: Question
Location: backend/src/main/java/nz/waiwatts/api/mbie/MbieGenerationQuarterlyController.java:48 — Class `MbieGenerationQuarterlyController`, method `getQuarterly(...)`
Description:
quarter is validated to 1..4, but do we also want explicit documented behavior when `quarter` is omitted (all quarters vs latest quarter)?
Status: Unresolved

### Batch B2 — LAWA Read Services

#### Critical
### Finding B2-01
Category: Critical
Location: backend/src/main/java/nz/waiwatts/service/lawa/LawaStateMultiYearReadServiceImpl.java:31 — Class `LawaStateMultiYearReadServiceImpl`, method `find(...)`
Description:
returns full list from repository with no pagination/limit contract at service boundary, preserving unbounded read behavior from controller.
Status: Unresolved

### Finding B2-02
Category: Critical
Location: backend/src/main/java/nz/waiwatts/service/lawa/LawaTrendMultiYearReadServiceImpl.java:29 — Class `LawaTrendMultiYearReadServiceImpl`, method `find(...)`
Description:
same unbounded read behavior.
Status: Unresolved

#### Improvement
### Finding B2-03
Category: Improvement
Location: backend/src/main/java/nz/waiwatts/service/lawa/LawaStateMultiYearReadServiceImpl.java:28 — Class `LawaStateMultiYearReadServiceImpl`, method `find(...)`
Description:
normalizes whitespace but does not normalize case for `indicator`/`region`; behavior correctness depends entirely on repository query semantics and may be surprising if case-sensitive.
Status: Unresolved

### Finding B2-04
Category: Improvement
Location: backend/src/main/java/nz/waiwatts/service/lawa/LawaTrendMultiYearReadServiceImpl.java:26 — Class `LawaTrendMultiYearReadServiceImpl`, method `find(...)`
Description:
same normalization pattern and dependency on repository collation/query behavior.
Status: Unresolved

### Finding B2-05
Category: Improvement
Location: backend/src/main/java/nz/waiwatts/service/lawa/LawaStateMultiYearReadServiceImpl.java:42 — Class `LawaStateMultiYearReadServiceImpl`, method `toDto(LawaStateMultiYearRecord)`
Description:
only `datasetRelease.id` is exposed (`releaseId`) with no complementary release metadata (published date/content hash/version semantics), which can make release behavior less transparent to API consumers.
Status: Unresolved

### Finding B2-06
Category: Improvement
Location: backend/src/main/java/nz/waiwatts/service/lawa/LawaTrendMultiYearReadServiceImpl.java:37 — Class `LawaTrendMultiYearReadServiceImpl`, method `toDto(LawaTrendMultiYearRecord)`
Description:
same release transparency limitation.
Status: Unresolved

#### Nice-to-have
### Finding B2-07
Category: Nice-to-have
Location: backend/src/main/java/nz/waiwatts/service/lawa/LawaStateMultiYearReadServiceImpl.java:31 — Class `LawaStateMultiYearReadServiceImpl`, method `find(...)`
Description:
uses stream mapping with `Collectors.toList()`; for very large result sets, memory footprint scales with full materialization (already inherited from list-return API contract).
Status: Unresolved

### Finding B2-08
Category: Nice-to-have
Location: backend/src/main/java/nz/waiwatts/service/lawa/LawaTrendMultiYearReadServiceImpl.java:29 — Class `LawaTrendMultiYearReadServiceImpl`, method `find(...)`
Description:
same large-list materialization pattern.
Status: Unresolved

#### Questions
### Finding B2-09
Category: Question
Location: backend/src/main/java/nz/waiwatts/service/lawa/LawaStateMultiYearReadServiceImpl.java:28 — Class `LawaStateMultiYearReadServiceImpl`, method `find(...)`
Description:
is indicator/region matching intentionally case-insensitive and enforced in repository query, or should service normalize case explicitly?
Status: Unresolved

### Finding B2-10
Category: Question
Location: backend/src/main/java/nz/waiwatts/service/lawa/LawaTrendMultiYearReadServiceImpl.java:26 — Class `LawaTrendMultiYearReadServiceImpl`, method `find(...)`
Description:
same question for trend service.
Status: Unresolved

### Finding B2-11
Category: Question
Location: backend/src/main/java/nz/waiwatts/service/lawa/LawaStateMultiYearReadService.java:10 — Interface `LawaStateMultiYearReadService`, method `find(...)`
Description:
should pagination be part of service contract for browse endpoints, or intentionally omitted for current dataset size?
Status: Unresolved

### Batch B3 — MBIE Read Services

#### Critical
### Finding B3-01
Category: Critical
Location: backend/src/main/java/nz/waiwatts/service/mbie/MbieGenerationAnnualReadServiceImpl.java:35 — Class `MbieGenerationAnnualReadServiceImpl`, method `find(...)`
Description:
returns full list without pagination/limit contract, preserving unbounded read behavior for browse endpoint.
Status: Unresolved

### Finding B3-02
Category: Critical
Location: backend/src/main/java/nz/waiwatts/service/mbie/MbieGenerationQuarterlyReadServiceImpl.java:34 — Class `MbieGenerationQuarterlyReadServiceImpl`, method `find(...)`
Description:
same unbounded read behavior.
Status: Unresolved

#### Improvement
### Finding B3-03
Category: Improvement
Location: backend/src/main/java/nz/waiwatts/service/mbie/MbieGenerationAnnualReadServiceImpl.java:40 — Class `MbieGenerationAnnualReadServiceImpl`, method `toDto(MbieGenerationAnnualRecord)`
Description:
exposes only `datasetRelease.id` as release signal; no additional release/version context is surfaced at this layer.
Status: Unresolved

### Finding B3-04
Category: Improvement
Location: backend/src/main/java/nz/waiwatts/service/mbie/MbieGenerationQuarterlyReadServiceImpl.java:39 — Class `MbieGenerationQuarterlyReadServiceImpl`, method `toDto(MbieGenerationQuarterlyRecord)`
Description:
same release/version transparency limitation.
Status: Unresolved

### Finding B3-05
Category: Improvement
Location: backend/src/main/java/nz/waiwatts/service/mbie/MbieGenerationAnnualReadServiceImpl.java:33 — Class `MbieGenerationAnnualReadServiceImpl`, method `find(...)`
Description:
input normalization uppercases `fuelType` but does not collapse internal whitespace; behavior for values like "natural   gas" depends on repository/query matching.
Status: Unresolved

### Finding B3-06
Category: Improvement
Location: backend/src/main/java/nz/waiwatts/service/mbie/MbieGenerationQuarterlyReadServiceImpl.java:32 — Class `MbieGenerationQuarterlyReadServiceImpl`, method `find(...)`
Description:
same normalization nuance.
Status: Unresolved

#### Nice-to-have
### Finding B3-07
Category: Nice-to-have
Location: backend/src/main/java/nz/waiwatts/service/mbie/MbieGenerationAnnualReadServiceImpl.java:35 — Class `MbieGenerationAnnualReadServiceImpl`, method `find(...)`
Description:
stream + `Collectors.toList()` materializes full result set in memory (aligned with current list-return contract but still a scale foot-gun).
Status: Unresolved

### Finding B3-08
Category: Nice-to-have
Location: backend/src/main/java/nz/waiwatts/service/mbie/MbieGenerationQuarterlyReadServiceImpl.java:34 — Class `MbieGenerationQuarterlyReadServiceImpl`, method `find(...)`
Description:
same full materialization pattern.
Status: Unresolved

#### Questions
### Finding B3-09
Category: Question
Location: backend/src/main/java/nz/waiwatts/service/mbie/MbieGenerationAnnualReadServiceImpl.java:33 — Class `MbieGenerationAnnualReadServiceImpl`, method `find(...)`
Description:
should fuel-type normalization include whitespace collapsing to match other service normalization patterns?
Status: Unresolved

### Finding B3-10
Category: Question
Location: backend/src/main/java/nz/waiwatts/service/mbie/MbieGenerationQuarterlyReadServiceImpl.java:28 — Class `MbieGenerationQuarterlyReadServiceImpl`, method `find(...)`
Description:
when `quarter` is null, is returning all quarters intentional and documented behavior for consumers?
Status: Unresolved

### Finding B3-11
Category: Question
Location: backend/src/main/java/nz/waiwatts/service/mbie/MbieGenerationAnnualReadService.java:9 — Interface `MbieGenerationAnnualReadService`, method `find(...)`
Description:
should pagination be explicitly part of this service contract for browse API stability?
Status: Unresolved

### Batch B4 — LAWA DTOs (Response Shapes)

#### Critical
### Finding B4-01
Category: Critical
Location: backend/src/main/java/nz/waiwatts/api/lawa/dto/LawaStateMultiYearRecordDto.java:23 — Class `LawaStateMultiYearRecordDto`
Description:
`periodStartYear`/`periodEndYear` are primitive `int`; null/unknown period semantics cannot be represented in response contract and will coerce to `0` if ever missing.
Status: Unresolved

### Finding B4-02
Category: Critical
Location: backend/src/main/java/nz/waiwatts/api/lawa/dto/LawaTrendMultiYearRecordDto.java:21 — Class `LawaTrendMultiYearRecordDto`
Description:
same primitive year-field limitation.
Status: Unresolved

#### Improvement
### Finding B4-03
Category: Improvement
Location: backend/src/main/java/nz/waiwatts/api/lawa/dto/LawaStateMultiYearRecordDto.java:25 — Class `LawaStateMultiYearRecordDto`, field `releaseId`
Description:
exposes release UUID only, with no version/published-date/content-hash context; dataset release semantics remain opaque to browse clients.
Status: Unresolved

### Finding B4-04
Category: Improvement
Location: backend/src/main/java/nz/waiwatts/api/lawa/dto/LawaTrendMultiYearRecordDto.java:23 — Class `LawaTrendMultiYearRecordDto`, field `releaseId`
Description:
same semantics gap.
Status: Unresolved

### Finding B4-05
Category: Improvement
Location: backend/src/main/java/nz/waiwatts/api/lawa/dto/LawaRegionsResponseDto.java:10 — Class `LawaRegionsResponseDto`, field `regions`
Description:
collection wrappers exist for list endpoints while record endpoints return bare arrays from controllers, producing shape inconsistency across related LAWA endpoints.
Status: Unresolved

### Finding B4-06
Category: Improvement
Location: backend/src/main/java/nz/waiwatts/api/lawa/dto/LawaIndicatorsResponseDto.java:10 — Class `LawaIndicatorsResponseDto`, field `indicators`
Description:
same envelope inconsistency point.
Status: Unresolved

#### Nice-to-have
### Finding B4-07
Category: Nice-to-have
Location: backend/src/main/java/nz/waiwatts/api/lawa/dto/LawaStateMultiYearRecordDto.java:30 — Class `LawaStateMultiYearRecordDto`, all-args constructor
Description:
large positional constructor increases maintenance risk for field-order mistakes as schema evolves.
Status: Unresolved

### Finding B4-08
Category: Nice-to-have
Location: backend/src/main/java/nz/waiwatts/api/lawa/dto/LawaTrendMultiYearRecordDto.java:27 — Class `LawaTrendMultiYearRecordDto`, all-args constructor
Description:
same large positional constructor risk.
Status: Unresolved

#### Questions
### Finding B4-09
Category: Question
Location: backend/src/main/java/nz/waiwatts/api/lawa/dto/LawaStateMultiYearRecordDto.java:23 — Class `LawaStateMultiYearRecordDto`
Description:
are period years guaranteed non-null at all times, or should nullable wrapper types be used to preserve unknown-period semantics?
Status: Unresolved

### Finding B4-10
Category: Question
Location: backend/src/main/java/nz/waiwatts/api/lawa/dto/LawaTrendMultiYearRecordDto.java:23 — Class `LawaTrendMultiYearRecordDto`
Description:
same question for trend records.
Status: Unresolved

### Finding B4-11
Category: Question
Location: backend/src/main/java/nz/waiwatts/api/lawa/dto/LawaRegionsResponseDto.java:10 — Class `LawaRegionsResponseDto`
Description:
do you want list endpoints and data endpoints to standardize on one envelope style in this slice, or is mixed shape intentional?
Status: Unresolved

### Batch B5 — MBIE DTOs (Response Shapes)

#### Critical
### Finding B5-01
Category: Critical
Location: backend/src/main/java/nz/waiwatts/api/mbie/dto/MbieGenerationAnnualRecordDto.java:7 — Class `MbieGenerationAnnualRecordDto`, field `periodYear`
Description:
primitive `int` cannot represent unknown/null period semantics and will coerce absent values to `0` if upstream data contract ever relaxes.
Status: Unresolved

### Finding B5-02
Category: Critical
Location: backend/src/main/java/nz/waiwatts/api/mbie/dto/MbieGenerationQuarterlyRecordDto.java:7 — Class `MbieGenerationQuarterlyRecordDto`, field `periodYear`
Description:
same primitive-period limitation.
Status: Unresolved

### Finding B5-03
Category: Critical
Location: backend/src/main/java/nz/waiwatts/api/mbie/dto/MbieGenerationQuarterlyRecordDto.java:8 — Class `MbieGenerationQuarterlyRecordDto`, field `periodQuarter`
Description:
same primitive-period limitation.
Status: Unresolved

#### Improvement
### Finding B5-04
Category: Improvement
Location: backend/src/main/java/nz/waiwatts/api/mbie/dto/MbieGenerationAnnualRecordDto.java:11 — Class `MbieGenerationAnnualRecordDto`, field `releaseId`
Description:
release UUID is exposed without version/published-date/content-hash context, making dataset release semantics opaque for API consumers.
Status: Unresolved

### Finding B5-05
Category: Improvement
Location: backend/src/main/java/nz/waiwatts/api/mbie/dto/MbieGenerationQuarterlyRecordDto.java:12 — Class `MbieGenerationQuarterlyRecordDto`, field `releaseId`
Description:
same semantics gap.
Status: Unresolved

### Finding B5-06
Category: Improvement
Location: backend/src/main/java/nz/waiwatts/api/mbie/dto/MbieFuelTypesResponseDto.java:10 — Class `MbieFuelTypesResponseDto`, field `fuelTypes`
Description:
wrapped list response shape for lookup endpoint differs from bare-array shape in record endpoints, mirroring cross-endpoint envelope inconsistency.
Status: Unresolved

#### Nice-to-have
### Finding B5-07
Category: Nice-to-have
Location: backend/src/main/java/nz/waiwatts/api/mbie/dto/MbieGenerationAnnualRecordDto.java:15 — Class `MbieGenerationAnnualRecordDto`, all-args constructor
Description:
positional constructor with multiple same-type fields increases accidental mapping-order risk as DTO evolves.
Status: Unresolved

### Finding B5-08
Category: Nice-to-have
Location: backend/src/main/java/nz/waiwatts/api/mbie/dto/MbieGenerationQuarterlyRecordDto.java:16 — Class `MbieGenerationQuarterlyRecordDto`, all-args constructor
Description:
same positional-constructor maintenance risk.
Status: Unresolved

#### Questions
### Finding B5-09
Category: Question
Location: backend/src/main/java/nz/waiwatts/api/mbie/dto/MbieGenerationAnnualRecordDto.java:11 — Class `MbieGenerationAnnualRecordDto`
Description:
should release semantics remain UUID-only, or should DTO carry additional release metadata for browse transparency?
Status: Unresolved

### Finding B5-10
Category: Question
Location: backend/src/main/java/nz/waiwatts/api/mbie/dto/MbieGenerationQuarterlyRecordDto.java:8 — Class `MbieGenerationQuarterlyRecordDto`
Description:
is quarter guaranteed non-null and always 1..4 at data contract level, or should nullable wrapper type be used defensively?
Status: Unresolved

### Finding B5-11
Category: Question
Location: backend/src/main/java/nz/waiwatts/api/mbie/dto/MbieFuelTypesResponseDto.java:10 — Class `MbieFuelTypesResponseDto`
Description:
is mixed envelope style (wrapped lookup responses vs unwrapped record lists) intentional across Slice B?
Status: Unresolved

### Batch B6 — Read API Repository Queries (Direct Dependencies)

#### Critical
### Finding B6-01
Category: Critical
Location: backend/src/main/java/nz/waiwatts/persistence/repositories/LawaStateMultiYearRecordRepository.java:23 — Interface `LawaStateMultiYearRecordRepository`, method `findForReadApi(...)`
Description:
query has no dataset-release scoping and no limit/pagination, so read API may blend rows across multiple releases and return unbounded result sets.
Status: Unresolved

### Finding B6-02
Category: Critical
Location: backend/src/main/java/nz/waiwatts/persistence/repositories/LawaTrendMultiYearRecordRepository.java:23 — Interface `LawaTrendMultiYearRecordRepository`, method `findForReadApi(...)`
Description:
same no-release-scope + unbounded result behavior.
Status: Unresolved

### Finding B6-03
Category: Critical
Location: backend/src/main/java/nz/waiwatts/persistence/repositories/MbieGenerationAnnualRecordRepository.java:22 — Interface `MbieGenerationAnnualRecordRepository`, method `findForReadApi(...)`
Description:
same no-release-scope + unbounded result behavior.
Status: Unresolved

### Finding B6-04
Category: Critical
Location: backend/src/main/java/nz/waiwatts/persistence/repositories/MbieGenerationQuarterlyRecordRepository.java:23 — Interface `MbieGenerationQuarterlyRecordRepository`, method `findForReadApi(...)`
Description:
same no-release-scope + unbounded result behavior.
Status: Unresolved

#### Improvement
### Finding B6-05
Category: Improvement
Location: backend/src/main/java/nz/waiwatts/persistence/repositories/LawaStateMultiYearRecordRepository.java:19 — Interface `LawaStateMultiYearRecordRepository`, method `findForReadApi(...)`
Description:
case-insensitive filter uses `LOWER(column) = LOWER(param)`; this can defeat normal index usage and is a potential performance foot-gun at scale.
Status: Unresolved

### Finding B6-06
Category: Improvement
Location: backend/src/main/java/nz/waiwatts/persistence/repositories/LawaTrendMultiYearRecordRepository.java:19 — Interface `LawaTrendMultiYearRecordRepository`, method `findForReadApi(...)`
Description:
same performance foot-gun pattern.
Status: Unresolved

### Finding B6-07
Category: Improvement
Location: backend/src/main/java/nz/waiwatts/persistence/repositories/MbieGenerationAnnualRecordRepository.java:19 — Interface `MbieGenerationAnnualRecordRepository`, method `findForReadApi(...)`
Description:
`UPPER(column) = UPPER(param)` has same potential index-avoidance issue.
Status: Unresolved

### Finding B6-08
Category: Improvement
Location: backend/src/main/java/nz/waiwatts/persistence/repositories/MbieGenerationQuarterlyRecordRepository.java:20 — Interface `MbieGenerationQuarterlyRecordRepository`, method `findForReadApi(...)`
Description:
same potential index-avoidance issue.
Status: Unresolved

#### Nice-to-have
### Finding B6-09
Category: Nice-to-have
Location: backend/src/main/java/nz/waiwatts/persistence/repositories/LawaStateMultiYearRecordRepository.java:28 — Interface `LawaStateMultiYearRecordRepository`, method `findDistinctRegionOrderByRegion()`
Description:
distinct lookups do not explicitly filter nulls; response may include null entries depending on data quality.
Status: Unresolved

### Finding B6-10
Category: Nice-to-have
Location: backend/src/main/java/nz/waiwatts/persistence/repositories/MbieGenerationAnnualRecordRepository.java:26 — Interface `MbieGenerationAnnualRecordRepository`, method `findDistinctFuelTypeNormOrderByFuelTypeNorm()`
Description:
same potential null-entry behavior for distinct lists.
Status: Unresolved

#### Questions
### Finding B6-11
Category: Question
Location: backend/src/main/java/nz/waiwatts/persistence/repositories/MbieGenerationAnnualRecordRepository.java:22 — Interface `MbieGenerationAnnualRecordRepository`, method `findForReadApi(...)`
Description:
is mixing across multiple dataset releases intentional for browse endpoints, or should queries be pinned to a canonical/latest release?
Status: Unresolved

### Finding B6-12
Category: Question
Location: backend/src/main/java/nz/waiwatts/persistence/repositories/LawaStateMultiYearRecordRepository.java:23 — Interface `LawaStateMultiYearRecordRepository`, method `findForReadApi(...)`
Description:
should period overlap semantics (`periodEndYear >= fromYear` and `periodStartYear <= toYear`) be considered the canonical filter behavior for multi-year windows?
Status: Unresolved

### Finding B6-13
Category: Question
Location: backend/src/main/java/nz/waiwatts/persistence/repositories/LawaTrendMultiYearRecordRepository.java:28 — Interface `LawaTrendMultiYearRecordRepository`, method `findDistinctRegionOrderByRegion()`
Description:
do clients expect null-safe value lists, or is raw distinct output intentional?
Status: Unresolved

### Batch B7 — Dataset Controller Tests (Behavior Coverage)

#### Critical
### Finding B7-01
Category: Critical
Location: backend/src/test/java/nz/waiwatts/api/lawa/LawaStateMultiYearControllerTest.java:64 — Class `LawaStateMultiYearControllerTest`, method `getGeneration_validatesFromTo()`
Description:
tests only `fromYear/toYear` validation; no tests cover invalid/blank `indicator`/`region`, so 4xx/5xx behavior for those inputs is currently unguarded.
Status: Unresolved

### Finding B7-02
Category: Critical
Location: backend/src/test/java/nz/waiwatts/api/lawa/LawaTrendMultiYearControllerTest.java:67 — Class `LawaTrendMultiYearControllerTest`, method `getTrend_validatesFromTo()`
Description:
same input-validation coverage gap.
Status: Unresolved

### Finding B7-03
Category: Critical
Location: backend/src/test/java/nz/waiwatts/api/mbie/MbieGenerationAnnualControllerTest.java:50 — Class `MbieGenerationAnnualControllerTest`, method `getGeneration_validatesFromTo()`
Description:
no tests cover malformed `fuelType` edge cases (blank/mixed whitespace/case), leaving matching behavior implicit.
Status: Unresolved

#### Improvement
### Finding B7-04
Category: Improvement
Location: backend/src/test/java/nz/waiwatts/api/lawa/LawaStateMultiYearControllerTest.java:33 — Class `LawaStateMultiYearControllerTest`, method `getGeneration_returnsOk_withPayload()`
Description:
test suite does not cover `/state/multiyear/regions` and `/state/multiyear/indicators` endpoints, so lookup-response shape/ordering is not contract-protected.
Status: Unresolved

### Finding B7-05
Category: Improvement
Location: backend/src/test/java/nz/waiwatts/api/lawa/LawaTrendMultiYearControllerTest.java:33 — Class `LawaTrendMultiYearControllerTest`, method `getTrend_returnsOk_withPayload()`
Description:
same missing coverage for `/trend/multiyear/regions` and `/trend/multiyear/indicators`.
Status: Unresolved

### Finding B7-06
Category: Improvement
Location: backend/src/test/java/nz/waiwatts/api/mbie/MbieGenerationAnnualControllerTest.java:32 — Class `MbieGenerationAnnualControllerTest`, method `getGeneration_returnsOk_withPayload()`
Description:
missing explicit coverage for `/generation/annual/fuel-types` endpoint response shape.
Status: Unresolved

### Finding B7-07
Category: Improvement
Location: backend/src/test/java/nz/waiwatts/api/mbie/MbieGenerationQuarterlyControllerTest.java:31 — Class `MbieGenerationQuarterlyControllerTest`, method `getQuarterly_returnsOk_withPayload()`
Description:
missing explicit coverage for `/generation/quarterly/fuel-types` endpoint response shape.
Status: Unresolved

### Finding B7-08
Category: Improvement
Location: backend/src/test/java/nz/waiwatts/api/mbie/MbieGenerationQuarterlyControllerTest.java:50 — Class `MbieGenerationQuarterlyControllerTest`, method `getQuarterly_validatesParams()`
Description:
validates out-of-range quarter only; no test for boundary quarters `1` and `4` to lock accepted range contract.
Status: Unresolved

#### Nice-to-have
### Finding B7-09
Category: Nice-to-have
Location: backend/src/test/java/nz/waiwatts/api/lawa/LawaStateMultiYearControllerTest.java:33 — Class `LawaStateMultiYearControllerTest`, method naming
Description:
uses `getGeneration_*` naming in LAWA state tests, which is semantically inconsistent with endpoint domain and makes intent scanning noisier.
Status: Unresolved

### Finding B7-10
Category: Nice-to-have
Location: backend/src/test/java/nz/waiwatts/api/lawa/LawaTrendMultiYearControllerTest.java:33 — Class `LawaTrendMultiYearControllerTest`, method naming
Description:
similarly uses generic naming (`getTrend_*`) without explicit endpoint path semantics.
Status: Unresolved

#### Questions
### Finding B7-11
Category: Question
Location: backend/src/test/java/nz/waiwatts/api/lawa/LawaStateMultiYearControllerTest.java:64 — Class `LawaStateMultiYearControllerTest`
Description:
should LAWA controller tests enforce specific behavior for blank string filters (treat as null vs exact blank) as part of public API contract?
Status: Unresolved

### Finding B7-12
Category: Question
Location: backend/src/test/java/nz/waiwatts/api/mbie/MbieGenerationAnnualControllerTest.java:50 — Class `MbieGenerationAnnualControllerTest`
Description:
should tests assert behavior when no filters are provided (all rows vs bounded default) to pin current browse semantics?
Status: Unresolved

### Finding B7-13
Category: Question
Location: backend/src/test/java/nz/waiwatts/api/mbie/MbieGenerationQuarterlyControllerTest.java:50 — Class `MbieGenerationQuarterlyControllerTest`
Description:
should quarter omission behavior be explicitly asserted in tests (all quarters vs latest)?
Status: Unresolved

## Slice C — Backend Ingestion (CSV parsing → normalize → persist)

### Batch C1 — Entrypoints + Orchestrators + Core Utilities

#### Critical
### Finding C1-01
Category: Critical
Location: backend/src/main/java/nz/waiwatts/ingestion/core/DatasetIngestionService.java — Class `DatasetIngestionService`, method `createImportedRelease(DatasetSource, DatasetIngestionRequest)`
Description:
release status is transitioned to `IMPORTED` before dataset-specific parse/persist steps run, so lifecycle status is not tightly coupled to successful row persistence at this boundary.
Status: Unresolved

#### Improvement
### Finding C1-02
Category: Improvement
Location: backend/src/main/java/nz/waiwatts/ingestion/core/IngestionOrchestrator.java — Package `nz.waiwatts.ingestion.core` (orchestrator file)
Description:
currently a placeholder, while orchestration logic is duplicated across dataset ingestion classes and CLI switch routing (`ManualIngestionCommand`), reducing pipeline clarity.
Status: Unresolved

### Finding C1-03
Category: Improvement
Location: backend/src/main/java/nz/waiwatts/ingestion/mbie/MbieAnnualIngestion.java — Class `MbieAnnualIngestion`, methods `ingestFixture(...)` and `ingestFile(...)`
Description:
persists each row with `recordRepository.save(e)` inside a loop; this is an obvious write-amplification foot-gun compared to batched persistence used in LAWA ingestion classes.
Status: Unresolved

### Finding C1-04
Category: Improvement
Location: backend/src/main/java/nz/waiwatts/ingestion/mbie/MbieQuarterlyIngestion.java — Class `MbieQuarterlyIngestion`, methods `ingestFixture(...)` and `ingestFile(...)`
Description:
same row-by-row persistence pattern.
Status: Unresolved

### Finding C1-05
Category: Improvement
Location: backend/src/main/java/nz/waiwatts/cli/ManualIngestionCommand.java — Class `ManualIngestionCommand`, method `ingest(...)`
Description:
dataset routing is hard-coded via `switch` and duplicates dataset-code lists maintained elsewhere (`SUPPORTED_DATASET_CODES`), increasing drift risk as ingestion surface grows.
Status: Unresolved

### Finding C1-06
Category: Improvement
Location: backend/src/main/java/nz/waiwatts/cli/ManualTransformCommand.java — Class `ManualTransformCommand`, methods `run(...)` and `validateDatasetSource(...)`
Description:
repeats hard-coded dataset source codes already represented in ingestion path, creating another drift point between transform and ingest entrypoints.
Status: Unresolved

### Finding C1-07
Category: Improvement
Location: backend/src/main/java/nz/waiwatts/ingestion/mbie/MbieAnnualIngestion.java — Class `MbieAnnualIngestion`, method `sha256Hex(byte[])`
Description:
duplicates hashing utility logic that also exists in `FileIngestionUtil.sha256Hex(byte[])` (and similarly in other ingestion classes), increasing maintenance duplication.
Status: Unresolved

### Finding C1-08
Category: Improvement
Location: backend/src/main/java/nz/waiwatts/ingestion/mbie/MbieQuarterlyIngestion.java — Class `MbieQuarterlyIngestion`, method `sha256Hex(byte[])`
Description:
same duplication.
Status: Unresolved

### Finding C1-09
Category: Improvement
Location: backend/src/main/java/nz/waiwatts/ingestion/lawa/LawaStateMultiYearIngestion.java — Class `LawaStateMultiYearIngestion`, method `sha256Hex(byte[])`
Description:
same duplication.
Status: Unresolved

### Finding C1-10
Category: Improvement
Location: backend/src/main/java/nz/waiwatts/ingestion/lawa/LawaTrendMultiYearIngestion.java — Class `LawaTrendMultiYearIngestion`, method `sha256Hex(byte[])`
Description:
same duplication.
Status: Unresolved

#### Nice-to-have
### Finding C1-11
Category: Nice-to-have
Location: backend/src/main/java/nz/waiwatts/cli/ManualIngestionCommand.java — Class `ManualIngestionCommand`, method `validateFile(String)`
Description:
“unsafe path” checks are string-based (`..`, `~`) rather than canonical path policy checks; acceptable for a manual CLI but not a strong safety boundary.
Status: Unresolved

### Finding C1-12
Category: Nice-to-have
Location: backend/src/main/java/nz/waiwatts/ingestion/core/FileIngestionUtil.java — Class `FileIngestionUtil`, methods `validateFilePath(String)` and `readFileBytes(String)`
Description:
validation overlaps with CLI validation logic (`ManualIngestionCommand.validateFile`), creating small duplication in file-safety/readability rules.
Status: Unresolved

#### Questions
### Finding C1-13
Category: Question
Location: backend/src/main/java/nz/waiwatts/ingestion/core/DatasetIngestionService.java — Class `DatasetIngestionService`, method `createImportedRelease(DatasetSource, DatasetIngestionRequest)`
Description:
is `IMPORTED` intended to mean “release row created” or “domain rows successfully parsed and persisted”?
Status: Unresolved

### Finding C1-14
Category: Question
Location: backend/src/main/java/nz/waiwatts/ingestion/core/IngestionOrchestrator.java — Package `nz.waiwatts.ingestion.core` (orchestrator file)
Description:
is this intentionally deferred by phase constraints, with per-dataset services as the canonical runtime orchestrators for now?
Status: Unresolved

### Finding C1-15
Category: Question
Location: backend/src/main/java/nz/waiwatts/ingestion/mbie/MbieAnnualIngestion.java — Class `MbieAnnualIngestion`, methods `ingestFixture(...)` and `ingestFile(...)`
Description:
is row-by-row `save(...)` intentional for hooks/auditing reasons, or should MBIE match LAWA’s batched `saveAll(...)` pattern when safe?
Status: Unresolved

### Batch C2 — Parser Implementations + Normalization Placement

#### Critical
### Finding C2-01
Category: Critical
Location: backend/src/main/java/nz/waiwatts/ingestion/mbie/MbieGenerationAnnualCsvParser.java — Class `MbieGenerationAnnualCsvParser`, method `splitCsv(String)`
Description:
CSV parsing uses `line.split(",", -1)` despite a robust quoted-field parser existing in `ingestion.util.CsvParser`; MBIE annual parsing can mis-parse valid CSV rows containing quoted commas.
Status: Unresolved

### Finding C2-02
Category: Critical
Location: backend/src/main/java/nz/waiwatts/ingestion/mbie/MbieGenerationQuarterlyCsvParser.java — Class `MbieGenerationQuarterlyCsvParser`, method `splitCsv(String)`
Description:
same naive split behavior and same correctness risk for quoted-comma input.
Status: Unresolved

#### Improvement
### Finding C2-03
Category: Improvement
Location: backend/src/main/java/nz/waiwatts/ingestion/lawa/LawaStateMultiYearCsvParser.java — Class `LawaStateMultiYearCsvParser`, methods `normalizeIndicator(...)` and `normalizeState(...)`
Description:
parser trusts non-blank `indicator_norm` / `state_norm` from source files without canonicalizing to a constrained vocabulary, so normalization guarantees are partly delegated to upstream files.
Status: Unresolved

### Finding C2-04
Category: Improvement
Location: backend/src/main/java/nz/waiwatts/ingestion/lawa/LawaTrendMultiYearCsvParser.java — Class `LawaTrendMultiYearCsvParser`, methods `normalizeIndicator(...)` and `normalizeTrend(...)`
Description:
same trust-first normalization behavior for provided `indicator_norm` / `trend_norm` values.
Status: Unresolved

### Finding C2-05
Category: Improvement
Location: backend/src/main/java/nz/waiwatts/ingestion/lawa/LawaStateMultiYearCsvParser.java — Class `LawaStateMultiYearCsvParser`, methods `parseHeader(...)`, `getRequired(...)`, `getOptional(...)`, `isRowBlank(...)`
Description:
header/row utility logic is duplicated across all four CSV parser implementations.
Status: Unresolved

### Finding C2-06
Category: Improvement
Location: backend/src/main/java/nz/waiwatts/ingestion/lawa/LawaTrendMultiYearCsvParser.java — Class `LawaTrendMultiYearCsvParser`, methods `parseHeader(...)`, `getRequired(...)`, `getOptional(...)`, `isRowBlank(...)`
Description:
same duplication.
Status: Unresolved

### Finding C2-07
Category: Improvement
Location: backend/src/main/java/nz/waiwatts/ingestion/mbie/MbieGenerationAnnualCsvParser.java — Class `MbieGenerationAnnualCsvParser`, methods `parseHeader(...)`, `getRequired(...)`, `getOptional(...)`, `isRowBlank(...)`
Description:
same duplication.
Status: Unresolved

### Finding C2-08
Category: Improvement
Location: backend/src/main/java/nz/waiwatts/ingestion/mbie/MbieGenerationQuarterlyCsvParser.java — Class `MbieGenerationQuarterlyCsvParser`, methods `parseHeader(...)`, `getRequired(...)`, `getOptional(...)`, `isRowBlank(...)`
Description:
same duplication.
Status: Unresolved

### Finding C2-09
Category: Improvement
Location: backend/src/main/java/nz/waiwatts/ingestion/lawa/LawaStateMultiYearCsvParser.java — Class `LawaStateMultiYearCsvParser`, method `parse(InputStream)`
Description:
numeric parsing failures (`Integer.parseInt`, `BigDecimal` parse) surface as unchecked exceptions without consistent row-context wrapping, reducing operator debuggability.
Status: Unresolved

### Finding C2-10
Category: Improvement
Location: backend/src/main/java/nz/waiwatts/ingestion/lawa/LawaTrendMultiYearCsvParser.java — Class `LawaTrendMultiYearCsvParser`, method `parse(InputStream)`
Description:
same unchecked numeric parse failure behavior.
Status: Unresolved

### Finding C2-11
Category: Improvement
Location: backend/src/main/java/nz/waiwatts/ingestion/mbie/MbieGenerationAnnualCsvParser.java — Class `MbieGenerationAnnualCsvParser`, method `parse(InputStream)`
Description:
same unchecked numeric parse failure behavior.
Status: Unresolved

### Finding C2-12
Category: Improvement
Location: backend/src/main/java/nz/waiwatts/ingestion/mbie/MbieGenerationQuarterlyCsvParser.java — Class `MbieGenerationQuarterlyCsvParser`, method `parse(InputStream)`
Description:
same unchecked numeric parse failure behavior.
Status: Unresolved

#### Nice-to-have
### Finding C2-13
Category: Nice-to-have
Location: backend/src/main/java/nz/waiwatts/ingestion/lawa/LawaStateMultiYearCsvParser.java — Class `LawaStateMultiYearCsvParser`, method `createIndicatorMap()`
Description:
indicator mapping table is embedded in parser class, making central normalization policy harder to audit across ingestion datasets.
Status: Unresolved

### Finding C2-14
Category: Nice-to-have
Location: backend/src/main/java/nz/waiwatts/ingestion/lawa/LawaTrendMultiYearCsvParser.java — Class `LawaTrendMultiYearCsvParser`, method `createIndicatorMap()`
Description:
same policy-visibility issue.
Status: Unresolved

#### Questions
### Finding C2-15
Category: Question
Location: backend/src/main/java/nz/waiwatts/ingestion/mbie/MbieGenerationAnnualCsvParser.java — Class `MbieGenerationAnnualCsvParser`, method `splitCsv(String)`
Description:
are MBIE CSVs guaranteed to never contain quoted commas, or should parser behavior match LAWA/shared CSV handling for consistency?
Status: Unresolved

### Finding C2-16
Category: Question
Location: backend/src/main/java/nz/waiwatts/ingestion/lawa/LawaStateMultiYearCsvParser.java — Class `LawaStateMultiYearCsvParser`, method `normalizeIndicator(...)`
Description:
should non-blank upstream `indicator_norm` values be canonicalized/validated anyway to keep normalized domain values strictly bounded?
Status: Unresolved

### Finding C2-17
Category: Question
Location: backend/src/main/java/nz/waiwatts/ingestion/lawa/LawaTrendMultiYearCsvParser.java — Class `LawaTrendMultiYearCsvParser`, method `normalizeIndicator(...)`
Description:
intentional that trend indicator mapping vocabulary (`NITRATE_N`, `TOTAL_N`, etc.) differs from state mapping vocabulary (`NO3N`, `TON`, etc.) for similar source labels?
Status: Unresolved

### Batch C3 — XLSX Transformers + Shared Transform Utils

#### Critical
### Finding C3-01
Category: Critical
Location: backend/src/main/java/nz/waiwatts/ingestion/transform/XlsxTransformUtil.java — Class `XlsxTransformUtil`, method `parseQuarterFromCell(Cell)`
Description:
converts Excel date cells using `ZoneId.systemDefault()`, introducing environment-dependent quarter derivation near date boundaries.
Status: Unresolved

### Finding C3-02
Category: Critical
Location: backend/src/main/java/nz/waiwatts/ingestion/transform/mbie/MbieQuarterlyXlsxTransformer.java — Class `MbieQuarterlyXlsxTransformer`, method `parseYearFromCell(Cell)`
Description:
same system-default timezone date conversion can make year extraction environment-dependent.
Status: Unresolved

#### Improvement
### Finding C3-03
Category: Improvement
Location: backend/src/main/java/nz/waiwatts/ingestion/transform/lawa/LawaStateMultiYearXlsxTransformer.java — Class `LawaStateMultiYearXlsxTransformer`, methods `normalizeIndicator(String)` and `createIndicatorMap()`
Description:
indicator normalization mapping is duplicated here and again in `LawaStateMultiYearCsvParser`, creating drift risk between transform output and CSV-ingestion fallback.
Status: Unresolved

### Finding C3-04
Category: Improvement
Location: backend/src/main/java/nz/waiwatts/ingestion/transform/lawa/LawaTrendMultiYearXlsxTransformer.java — Class `LawaTrendMultiYearXlsxTransformer`, methods `normalizeIndicator(String)` and `createIndicatorMap()`
Description:
same duplication with `LawaTrendMultiYearCsvParser`.
Status: Unresolved

### Finding C3-05
Category: Improvement
Location: backend/src/main/java/nz/waiwatts/ingestion/transform/lawa/LawaTrendMultiYearXlsxTransformer.java — Class `LawaTrendMultiYearXlsxTransformer`, method `normalizeTrend(String)`
Description:
trend normalization logic is duplicated with `LawaTrendMultiYearCsvParser.normalizeTrend(...)`, creating another policy-drift point.
Status: Unresolved

### Finding C3-06
Category: Improvement
Location: backend/src/main/java/nz/waiwatts/ingestion/transform/lawa/LawaStateMultiYearXlsxTransformer.java — Class `LawaStateMultiYearXlsxTransformer`, method `normalizeState(String)`
Description:
band-to-state mapping is duplicated with `LawaStateMultiYearCsvParser.normalizeState(...)`.
Status: Unresolved

### Finding C3-07
Category: Improvement
Location: backend/src/main/java/nz/waiwatts/ingestion/transform/mbie/MbieAnnualXlsxTransformer.java — Class `MbieAnnualXlsxTransformer`, method `resolveFuelColumns(...)`
Description:
fallback behavior can include both synonyms (e.g., `oil` + `oil1`, `solar` + `solarpv`) if both columns exist, which risks ambiguous raw-fuel representation unless workbook shape is tightly controlled.
Status: Unresolved

### Finding C3-08
Category: Improvement
Location: backend/src/main/java/nz/waiwatts/ingestion/transform/XlsxTransformUtil.java — Class `XlsxTransformUtil`, method `normalizeHeader(String)`
Description:
aggressive punctuation stripping can collapse distinct header names into same key and silently keep first match, reducing diagnosability when workbook headers drift.
Status: Unresolved

#### Nice-to-have
### Finding C3-09
Category: Nice-to-have
Location: backend/src/main/java/nz/waiwatts/ingestion/transform/mbie/MbieAnnualXlsxTransformer.java — Class `MbieAnnualXlsxTransformer`, method `resolveFuelColumns(...)`
Description:
`missing` list is populated from all known fuel keys when no columns are found, but it does not report nearby/actual detected headers; troubleshooting malformed sheets will be slower.
Status: Unresolved

### Finding C3-10
Category: Nice-to-have
Location: backend/src/main/java/nz/waiwatts/ingestion/transform/lawa/LawaTrendMultiYearXlsxTransformer.java — Class `LawaTrendMultiYearXlsxTransformer`, method `transform(InputStream)`
Description:
writes `units` as empty string for all rows; acceptable for current schema, but this silently drops source-unit signals if they become available later.
Status: Unresolved

#### Questions
### Finding C3-11
Category: Question
Location: backend/src/main/java/nz/waiwatts/ingestion/transform/XlsxTransformUtil.java — Class `XlsxTransformUtil`, method `parseQuarterFromCell(Cell)`
Description:
should date-cell quarter/year extraction be timezone-fixed (e.g., UTC) to guarantee deterministic transforms across environments?
Status: Unresolved

### Finding C3-12
Category: Question
Location: backend/src/main/java/nz/waiwatts/ingestion/transform/mbie/MbieAnnualXlsxTransformer.java — Class `MbieAnnualXlsxTransformer`, method `resolveFuelColumns(...)`
Description:
if both synonym columns are present in a workbook, should both be emitted, or should one canonical source column win?
Status: Unresolved

### Finding C3-13
Category: Question
Location: backend/src/main/java/nz/waiwatts/ingestion/transform/lawa/LawaTrendMultiYearXlsxTransformer.java — Class `LawaTrendMultiYearXlsxTransformer`, method `transform(InputStream)`
Description:
is `period_start_year = asOfYear - trendPeriodYears` the intended window semantics, or should the interval be inclusive (`asOfYear - trendPeriodYears + 1`)?
Status: Unresolved

### Batch C4 — Parsed Record DTOs + Orchestrator Closure

#### Critical
### Finding C4-01
Category: Critical
Location: N/A
Description:
None.
Status: Unresolved

#### Improvement
### Finding C4-02
Category: Improvement
Location: backend/src/main/java/nz/waiwatts/ingestion/core/IngestionOrchestrator.java — Package `nz.waiwatts.ingestion.core` (orchestrator file)
Description:
remains a placeholder, confirming orchestration responsibilities are still distributed across dataset-specific ingestion services and CLI entrypoints.
Status: Unresolved

#### Nice-to-have
### Finding C4-03
Category: Nice-to-have
Location: backend/src/main/java/nz/waiwatts/ingestion/lawa/LawaStateMultiYearParsedRecord.java — Class `LawaStateMultiYearParsedRecord`, constructor and fields
Description:
long positional constructor with many same-type parameters makes accidental argument-order mistakes harder to spot during parser/transformer evolution.
Status: Unresolved

### Finding C4-04
Category: Nice-to-have
Location: backend/src/main/java/nz/waiwatts/ingestion/lawa/LawaTrendMultiYearParsedRecord.java — Class `LawaTrendMultiYearParsedRecord`, constructor and fields
Description:
same large positional-constructor readability/maintainability risk.
Status: Unresolved

#### Questions
### Finding C4-05
Category: Question
Location: backend/src/main/java/nz/waiwatts/ingestion/core/IngestionOrchestrator.java — Package `nz.waiwatts.ingestion.core` (orchestrator file)
Description:
should this placeholder be treated as intentional for Phase 15 closure, with no further orchestration abstraction expected in-slice?
Status: Unresolved

### Batch C5 — Ingestion Package Metadata

#### Critical
### Finding C5-01
Category: Critical
Location: N/A
Description:
None.
Status: Unresolved

#### Improvement
### Finding C5-02
Category: Improvement
Location: backend/src/main/java/nz/waiwatts/ingestion/package-info.java — Package `nz.waiwatts.ingestion`
Description:
package-level doc still describes a Step 2 scaffold placeholder and does not reflect the current multi-dataset ingestion implementation, which weakens top-level pipeline discoverability.
Status: Unresolved

#### Nice-to-have
### Finding C5-03
Category: Nice-to-have
Location: N/A
Description:
None.
Status: Unresolved

#### Questions
### Finding C5-04
Category: Question
Location: backend/src/main/java/nz/waiwatts/ingestion/package-info.java — Package `nz.waiwatts.ingestion`
Description:
should package-level documentation be updated in this phase to describe the actual parse/transform/ingest flow boundaries now present?
Status: Unresolved

Slice C discovery complete. No more files will be pulled into Slice C unless a later slice reveals a direct ingestion dependency.

## Slice D — Backend Persistence & Schema Alignment

### Batch D1 — Repository Interfaces + Core LAWA/MBIE Entities

#### Critical
### Finding D1-01
Category: Critical
Location: backend/src/main/java/nz/waiwatts/domain/lawa/LawaTrendMultiYearRecord.java — Class `LawaTrendMultiYearRecord`, methods `setTrendScore(Integer)` and `setTrendPeriodYears(Integer)`
Description:
entity marks `trend_score` and `trend_period_years` as `nullable = false` while the ingestion parser path can produce `null` values, creating a schema/entity contract violation risk at persist time.
Status: Unresolved

### Finding D1-02
Category: Critical
Location: backend/src/main/java/nz/waiwatts/persistence/repositories/DatasetReleaseRepository.java — Interface `DatasetReleaseRepository`, method `findFirstByDatasetSourceIdAndContentHash(UUID, String)`
Description:
`findFirst...` has no explicit ordering and entity schema shown here has no declared uniqueness constraint on `(dataset_source_id, content_hash)`; duplicate rows would make idempotency resolution non-deterministic.
Status: Unresolved

#### Improvement
### Finding D1-03
Category: Improvement
Location: backend/src/main/java/nz/waiwatts/domain/datasets/DatasetSource.java — Class `DatasetSource`, field mapping for `code`
Description:
column is `unique = true` but not `nullable = false`, while repository/service paths treat code as stable required key.
Status: Unresolved

### Finding D1-04
Category: Improvement
Location: backend/src/main/java/nz/waiwatts/persistence/repositories/DatasetReleaseRepository.java — Interface `DatasetReleaseRepository`, method `findByDatasetSourceId(UUID)`
Description:
no ordering contract is declared, so callers cannot rely on deterministic release chronology from this method alone.
Status: Unresolved

### Finding D1-05
Category: Improvement
Location: backend/src/main/java/nz/waiwatts/domain/mbie/MbieGenerationAnnualRecord.java — Class `MbieGenerationAnnualRecord`, method `getPeriodYear()` / field `periodYear`
Description:
uses boxed `Integer` for non-null DB column, while analogous quarterly entity uses primitive `int`; this inconsistency weakens model clarity.
Status: Unresolved

### Finding D1-06
Category: Improvement
Location: backend/src/main/java/nz/waiwatts/domain/lawa/LawaTrendMultiYearRecord.java — Class `LawaTrendMultiYearRecord`, field mapping for `units`
Description:
column exists but transformer path currently emits empty units values, so persistence stores a mostly non-informative column unless upstream input shape changes.
Status: Unresolved

### Finding D1-07
Category: Improvement
Location: backend/src/main/java/nz/waiwatts/persistence/repositories/LawaStateMultiYearRecordRepository.java — Interface `LawaStateMultiYearRecordRepository`, method `findForReadApi(...)`
Description:
query is deterministic via explicit `ORDER BY`, but remains unbounded and non-release-scoped at persistence layer.
Status: Unresolved

### Finding D1-08
Category: Improvement
Location: backend/src/main/java/nz/waiwatts/persistence/repositories/LawaTrendMultiYearRecordRepository.java — Interface `LawaTrendMultiYearRecordRepository`, method `findForReadApi(...)`
Description:
same unbounded + non-release-scoped behavior.
Status: Unresolved

### Finding D1-09
Category: Improvement
Location: backend/src/main/java/nz/waiwatts/persistence/repositories/MbieGenerationAnnualRecordRepository.java — Interface `MbieGenerationAnnualRecordRepository`, method `findForReadApi(...)`
Description:
same unbounded + non-release-scoped behavior.
Status: Unresolved

### Finding D1-10
Category: Improvement
Location: backend/src/main/java/nz/waiwatts/persistence/repositories/MbieGenerationQuarterlyRecordRepository.java — Interface `MbieGenerationQuarterlyRecordRepository`, method `findForReadApi(...)`
Description:
same unbounded + non-release-scoped behavior.
Status: Unresolved

#### Nice-to-have
### Finding D1-11
Category: Nice-to-have
Location: backend/src/main/java/nz/waiwatts/persistence/repositories/LawaStateMultiYearRecordRepository.java — Interface `LawaStateMultiYearRecordRepository`, method `findDistinctRegionOrderByRegion()`
Description:
distinct list query does not filter null values explicitly.
Status: Unresolved

### Finding D1-12
Category: Nice-to-have
Location: backend/src/main/java/nz/waiwatts/persistence/repositories/LawaTrendMultiYearRecordRepository.java — Interface `LawaTrendMultiYearRecordRepository`, method `findDistinctRegionOrderByRegion()`
Description:
same null-handling ambiguity.
Status: Unresolved

### Finding D1-13
Category: Nice-to-have
Location: backend/src/main/java/nz/waiwatts/persistence/repositories/MbieGenerationAnnualRecordRepository.java — Interface `MbieGenerationAnnualRecordRepository`, method `findDistinctFuelTypeNormOrderByFuelTypeNorm()`
Description:
same null-handling ambiguity.
Status: Unresolved

### Finding D1-14
Category: Nice-to-have
Location: backend/src/main/java/nz/waiwatts/persistence/repositories/MbieGenerationQuarterlyRecordRepository.java — Interface `MbieGenerationQuarterlyRecordRepository`, method `findDistinctFuelTypeNormOrderByFuelTypeNorm()`
Description:
same null-handling ambiguity.
Status: Unresolved

#### Questions
### Finding D1-15
Category: Question
Location: backend/src/main/java/nz/waiwatts/domain/lawa/LawaTrendMultiYearRecord.java — Class `LawaTrendMultiYearRecord`, columns `trend_score` / `trend_period_years`
Description:
should blanks from source data be rejected before persistence, or should these columns be nullable by design?
Status: Unresolved

### Finding D1-16
Category: Question
Location: backend/src/main/java/nz/waiwatts/persistence/repositories/DatasetReleaseRepository.java — Interface `DatasetReleaseRepository`, method `findFirstByDatasetSourceIdAndContentHash(UUID, String)`
Description:
is `(dataset_source_id, content_hash)` intended to be unique at DB level?
Status: Unresolved

### Finding D1-17
Category: Question
Location: backend/src/main/java/nz/waiwatts/domain/datasets/DatasetRelease.java — Class `DatasetRelease`, fields `releaseLabel`, `publishedDate`, `contentHash`
Description:
is there any explicit persistence-level `version` concept planned, or is release identity intentionally content-hash + metadata only?
Status: Unresolved

### Batch D2 — Migration Structure + Persistence Configuration

#### Critical
### Finding D2-01
Category: Critical
Location: backend/src/main/resources/db/migration/V12__lawa_trend_multi_year.sql — Migration `V12__lawa_trend_multi_year.sql`
Description:
DB unique index `uk_lawa_trend_window` is defined on `(dataset_release_id, lawa_site_id, indicator_raw, period_type, trend_period_years, period_end_year)`, while entity-level unique constraint in `LawaTrendMultiYearRecord` is declared on `indicator_norm` instead of `indicator_raw`; schema-vs-entity uniqueness semantics diverge.
Status: Unresolved

#### Improvement
### Finding D2-02
Category: Improvement
Location: backend/src/main/resources/db/migration/V13__insert_dataset_sources.sql — Migration `V13__insert_dataset_sources.sql`
Description:
dataset source seeding uses plain `INSERT` with fixed UUIDs and no idempotent guard (`ON CONFLICT`/equivalent), making reruns fragile where Flyway history is reset or manual replay occurs.
Status: Unresolved

### Finding D2-03
Category: Improvement
Location: backend/src/main/resources/db/migration/V8__dataset_source_taxonomy.sql — Migration `V8__dataset_source_taxonomy.sql`
Description:
update-by-publisher fallback (`publisher = 'MBIE' AND code NOT LIKE 'mbie.generation.%'`) can rewrite multiple MBIE source codes based on broad predicate, which is brittle as source catalog grows.
Status: Unresolved

### Finding D2-04
Category: Improvement
Location: backend/src/main/resources/db/migration/V14__phase15_database_indexing.sql — Migration `V14__phase15_database_indexing.sql`
Description:
index strategy is read-focused and clear, but no explicit indexes are added for dataset release lineage lookup patterns (`dataset_release.dataset_source_id`, `dataset_release(dataset_source_id, content_hash)`) beyond unique constraint lineage from V2.
Status: Unresolved

### Finding D2-05
Category: Improvement
Location: backend/src/main/resources/application.yml — Config `spring.flyway.enabled=true` with default `spring.profiles.active=dev`
Description:
local defaults are fine, but this file does not declare explicit Flyway locations/table/baseline behavior, so migration behavior relies on framework defaults.
Status: Unresolved

### Finding D2-06
Category: Improvement
Location: backend/src/main/resources/db/migration/V1__baseline.sql — Migration `V1__baseline.sql`
Description:
effectively empty baseline marker is valid but provides no schema bootstrap context by itself, reducing standalone readability for reviewers.
Status: Unresolved

#### Nice-to-have
### Finding D2-07
Category: Nice-to-have
Location: backend/src/main/resources/db/migration/V10__mbie_generation_quarterly.sql — Migration `V10__mbie_generation_quarterly.sql`
Description:
includes helpful explanatory comments and explicit read index; this is a positive consistency signal relative to less-commented migrations.
Status: Unresolved

### Finding D2-08
Category: Nice-to-have
Location: backend/src/main/resources/db/migration/V14__phase15_database_indexing.sql — Migration `V14__phase15_database_indexing.sql`
Description:
includes targeted `DROP INDEX IF EXISTS` cleanup, which improves migration hygiene and avoids obsolete-index drift.
Status: Unresolved

#### Questions
### Finding D2-09
Category: Question
Location: N/A
Description:
`backend/src/main/resources/db/migration/V12__lawa_trend_multi_year.sql` and `backend/src/main/java/nz/waiwatts/domain/lawa/LawaTrendMultiYearRecord.java` — Migration vs entity: should uniqueness key use `indicator_raw` (as DB migration currently does) or `indicator_norm` (as entity annotation currently does)?
Status: Unresolved

### Finding D2-10
Category: Question
Location: backend/src/main/resources/db/migration/V13__insert_dataset_sources.sql — Migration `V13__insert_dataset_sources.sql`
Description:
is non-idempotent seed insertion intentional because environments are always advanced via monotonic Flyway history only?
Status: Unresolved

### Finding D2-11
Category: Question
Location: backend/src/main/resources/db/migration/ — Per Phase 15 instruction from user
Description:
schema-documentation crosscheck is intentionally skipped in this task due to known doc staleness; should this be treated as accepted scope exclusion for Slice D closure?
Status: Unresolved

### Batch D3 — Environment Persistence Config (Dev/Test)

#### Critical
### Finding D3-01
Category: Critical
Location: N/A
Description:
None.
Status: Unresolved

#### Improvement
### Finding D3-02
Category: Improvement
Location: backend/src/test/resources/application-test.yml — Test config `spring.datasource.url` (`MODE=PostgreSQL`) + `spring.flyway.enabled=true`
Description:
test profile runs persistence against H2 compatibility mode, so SQL/constraint behavior can diverge from production PostgreSQL in edge cases.
Status: Unresolved

### Finding D3-03
Category: Improvement
Location: backend/src/main/resources/application-dev.yml — Dev config
Description:
relies entirely on defaults for Flyway location/history/baseline strategy; operationally acceptable, but migration behavior assumptions are implicit rather than explicit.
Status: Unresolved

#### Nice-to-have
### Finding D3-04
Category: Nice-to-have
Location: N/A
Description:
`backend/src/main/resources/application-dev.yml` and `backend/src/test/resources/application-test.yml` — both keep persistence config concise and aligned on `flyway.enabled=true`, which is a good consistency signal.
Status: Unresolved

#### Questions
### Finding D3-05
Category: Question
Location: backend/src/test/resources/application-test.yml — Test profile
Description:
do you want a periodic sanity check against real Postgres (outside this slice) to catch H2/Postgres drift in DDL/query semantics?
Status: Unresolved

### Finding D3-06
Category: Question
Location: backend/src/main/resources/application-dev.yml — Dev profile
Description:
should Flyway defaults remain implicit by policy, or be pinned explicitly for portfolio clarity?
Status: Unresolved

Slice D discovery complete. No more files will be pulled into Slice D unless a later slice reveals a direct persistence dependency.

## Slice E — Frontend Ask UX + Citation Display

### Batch E1 — Ask Page + Explanations API Client

#### Critical
### Finding E1-01
Category: Critical
Location: frontend/src/features/results/ResultsPage.tsx — Component `ResultsPage`
Description:
when route state is missing (e.g., direct `/results` load, refresh, or back/forward state loss), component falls into the final branch and shows an indefinite “Processing your question...” spinner without active request, creating a dead-end state that requires manual navigation.
Status: Unresolved

#### Improvement
### Finding E1-02
Category: Improvement
Location: frontend/src/features/ask/AskPage.tsx — Component `AskPage`, function `handleSubmit(...)`
Description:
catch block collapses all failures to generic copy (“Failed to process question. Please try again.”), so network/server/misconfiguration conditions are not distinguished for users.
Status: Unresolved

### Finding E1-03
Category: Improvement
Location: frontend/src/features/ask/AskPage.tsx — Component `AskPage`, function `handleSubmit(...)`
Description:
does not branch on `HttpError` details produced by API client, leaving the no-LLM/misconfiguration path without clear operator guidance.
Status: Unresolved

### Finding E1-04
Category: Improvement
Location: frontend/src/features/results/ResultsPage.tsx — Component `ResultsPage`, refusal branch rendering
Description:
refusal message is displayed, but refusal code is shown as raw backend code string with no user-friendly mapping/explanation.
Status: Unresolved

### Finding E1-05
Category: Improvement
Location: frontend/src/features/results/ResultsPage.tsx — Component `ResultsPage`, citations rendering block
Description:
citations are rendered as raw IDs and optional raw fields only; there is no dataset/source-friendly labeling or grouping, reducing usefulness for new users.
Status: Unresolved

### Finding E1-06
Category: Improvement
Location: frontend/src/features/results/ResultsPage.tsx — Component `ResultsPage`, citations conditional
Description:
outer condition `citations.length > 0` makes inner `citations.length === 0` branch unreachable, leaving minor dead code in citation panel logic.
Status: Unresolved

### Finding E1-07
Category: Improvement
Location: N/A
Description:
`frontend/src/features/ask/AskPage.tsx` and `frontend/src/features/results/ResultsPage.tsx` — Ask flow state handoff via navigation `location.state`: state machine depends on transient router state rather than durable request/result state, which is fragile across refresh/deep-link behavior.
Status: Unresolved

#### Nice-to-have
### Finding E1-08
Category: Nice-to-have
Location: frontend/src/features/results/ResultsPage.tsx — Component `ResultsPage`, citations list `key={index}`
Description:
index-based keys are acceptable for static lists but less stable than citation ID keys if ordering changes.
Status: Unresolved

### Finding E1-09
Category: Nice-to-have
Location: frontend/src/features/ask/AskPage.tsx
Description:
Component `AskPage`, `exampleQuestions` includes two very similar trend prompts; prompt set variety could be broadened for portfolio polish.
Status: Unresolved

#### Questions
### Finding E1-10
Category: Question
Location: frontend/src/features/results/ResultsPage.tsx — Component `ResultsPage`
Description:
for missing route state, should UX redirect back to `/ask` with a clear notice instead of showing a loading spinner?
Status: Unresolved

### Finding E1-11
Category: Question
Location: frontend/src/features/ask/AskPage.tsx — Component `AskPage`, function `handleSubmit(...)`
Description:
should UI explicitly distinguish refusal (expected), backend error, and network failure in Ask-page messaging before navigation?
Status: Unresolved

### Finding E1-12
Category: Question
Location: frontend/src/api/client.ts — Class `ApiClient`, method `request(...)`
Description:
this already classifies errors (`HttpError` + diagnostics); should Ask UI surface this classification directly for operator clarity in Phase 15?
Status: Unresolved

### Batch E2 — Refusal/Error Components + Diagnostics + Route Wiring

#### Critical
### Finding E2-01
Category: Critical
Location: frontend/src/components/DiagnosticsPanel.tsx — Component `DiagnosticsPanel`
Description:
diagnostics are loaded once into local state (`useState(getDiagnostics())`) and never refreshed; the panel can show stale data and `Clear` does not update visible rows until remount, reducing reliability of Ask debugging during failure/refusal triage.
Status: Unresolved

#### Improvement
### Finding E2-02
Category: Improvement
Location: frontend/src/App.tsx — Component `App`, route `/results`
Description:
route is publicly accessible with no guard/redirect despite `ResultsPage` depending on transient navigation state, which contributes to dead-end Ask flow states on direct entry/refresh.
Status: Unresolved

### Finding E2-03
Category: Improvement
Location: frontend/src/utils/apiUtils.ts — function `classifyError(error)`
Description:
CORS classification relies on `TypeError.message.includes('CORS')`, which is typically unavailable in browser fetch failures; most CORS failures will be reported as generic NETWORK.
Status: Unresolved

### Finding E2-04
Category: Improvement
Location: frontend/src/components/ui/Callout.tsx — Component `RefusalCallout`
Description:
refusal presentation supports only raw message text with static title (“Unable to Answer”); no slot for refusal code/category guidance, limiting refusal explainability in Ask results.
Status: Unresolved

### Finding E2-05
Category: Improvement
Location: frontend/src/components/DiagnosticsPanel.tsx
Description:
Component `DiagnosticsPanel`, list rendering uses `key={index}` and no request-scoped stable key, making panel updates harder to reason about when records roll over.
Status: Unresolved

#### Nice-to-have
### Finding E2-06
Category: Nice-to-have
Location: frontend/src/App.tsx
Description:
file tail contains stray comment `// Test change`, which is a minor polish issue in portfolio-facing application wiring.
Status: Unresolved

### Finding E2-07
Category: Nice-to-have
Location: frontend/src/utils/diagnostics.ts
Description:
module-level mutable array store is simple and effective for dev diagnostics, but lacks subscription semantics for live panel updates.
Status: Unresolved

#### Questions
### Finding E2-08
Category: Question
Location: N/A
Description:
`frontend/src/App.tsx` and `frontend/src/features/results/ResultsPage.tsx` — should `/results` be navigable only from Ask submission, with route guard fallback to `/ask` when state is absent?
Status: Unresolved

### Finding E2-09
Category: Question
Location: frontend/src/components/ui/Callout.tsx
Description:
should refusal UI include an optional structured hint area (e.g., mapped by refusal code) to separate expected refusal from actionable errors?
Status: Unresolved

### Finding E2-10
Category: Question
Location: frontend/src/components/DiagnosticsPanel.tsx
Description:
is diagnostics panel expected to be live-updating during request flow, or intentionally snapshot-only for now?
Status: Unresolved

### Batch E3 — Layout / Logging / Ask-Adjacent Utility Wiring

#### Critical
### Finding E3-01
Category: Critical
Location: N/A
Description:
None.
Status: Unresolved

#### Improvement
### Finding E3-02
Category: Improvement
Location: frontend/src/components/LogInstructions.tsx — Component `LogInstructions`
Description:
component is Ask-supportive (misconfiguration/CORS guidance) but currently not mounted anywhere in app flow, so no-LLM/misconfiguration guidance path is effectively unavailable to users.
Status: Unresolved

### Finding E3-03
Category: Improvement
Location: frontend/src/components/DiagnosticsPanel.tsx — Component `DiagnosticsPanel` and hook `useDiagnostics()`
Description:
diagnostics UI utilities are defined but not used in routed Ask UI, so client-side diagnostics collected by `apiClient` are not visible in normal Ask troubleshooting flow.
Status: Unresolved

### Finding E3-04
Category: Improvement
Location: frontend/src/components/LogInstructions.tsx — Component `LogInstructions`, static “Current Issue” section
Description:
embeds a hard-coded environment-specific CORS troubleshooting statement, which can become stale and reduce portfolio credibility if surfaced later.
Status: Unresolved

### Finding E3-05
Category: Improvement
Location: frontend/src/utils/logger.ts — Class `ConsoleLogger`, methods `warn(...)` / `error(...)`
Description:
non-dev mode strips structured args, reducing production/preview observability for Ask failures.
Status: Unresolved

#### Nice-to-have
### Finding E3-06
Category: Nice-to-have
Location: frontend/src/components/Layout.tsx — Component `Layout`
Description:
clean shared shell with predictable content container; good consistency signal for Ask and Results pages.
Status: Unresolved

### Finding E3-07
Category: Nice-to-have
Location: frontend/src/components/ui/index.ts
Description:
UI barrel export is concise and keeps callout/error components discoverable for Ask/Results usage.
Status: Unresolved

#### Questions
### Finding E3-08
Category: Question
Location: frontend/src/components/LogInstructions.tsx
Description:
should this component be intentionally dormant for now, or is it expected to be reachable from Ask error states in Phase 15 UX?
Status: Unresolved

### Finding E3-09
Category: Question
Location: frontend/src/components/DiagnosticsPanel.tsx
Description:
should diagnostics tooling remain internal-only, or be explicitly wired behind a dev toggle in layout for easier Ask debugging?
Status: Unresolved

Slice E discovery complete. No more files will be pulled into Slice E unless a later slice reveals a direct Ask UX dependency.

## Slice F — Frontend Dataset Pages Consistency (LAWA + MBIE)

### Batch F1 — LAWA + MBIE Pages + Dataset Routing

#### Critical
### Finding F1-01
Category: Critical
Location: frontend/src/features/browse-lawa/LawaBrowsePage.tsx — Component `LawaBrowsePage`, table row render inside `displayData.map(...)`
Description:
`p95` column cell is conditionally omitted per-row (`row.p95 !== null && row.p95 !== -99`) while header inclusion is decided globally, causing column misalignment within the same table when some rows lack `p95`.
Status: Unresolved

#### Improvement
### Finding F1-02
Category: Improvement
Location: N/A
Description:
`frontend/src/features/browse-lawa/LawaBrowsePage.tsx` vs `frontend/src/features/browse-mbie/MbieBrowsePage.tsx` — Components `LawaBrowsePage` and `MbieBrowsePage`: filter/load model is inconsistent (LAWA hard-gates data on region/indicator selection; MBIE loads full dataset by default), creating different mental models for “default view” between browse pages.
Status: Unresolved

### Finding F1-03
Category: Improvement
Location: frontend/src/features/browse-mbie/MbieBrowsePage.tsx — Component `MbieBrowsePage`, chart branch (`activeTab === 'chart'`)
Description:
no explicit loading/error/empty messaging wrapper around chart panel; users can see a blank/near-blank chart area while table tab has clear loading/error/empty states.
Status: Unresolved

### Finding F1-04
Category: Improvement
Location: frontend/src/features/browse-lawa/LawaBrowsePage.tsx — Component `LawaBrowsePage`, terminology/copy
Description:
uses “State”/“Trend”/“Attribute Band” labels and gated prompt text; MBIE page uses “View Type”/“Fuel Types”/“Show Total” idioms, with no shared pattern language for equivalent filter intents.
Status: Unresolved

### Finding F1-05
Category: Improvement
Location: N/A
Description:
`frontend/src/features/browse-lawa/LawaBrowsePage.tsx` and `frontend/src/features/browse-mbie/MbieBrowsePage.tsx` — both pages expose “Explain This Data” actions, but prefill question construction differs significantly (LAWA contextual phrase assembly vs MBIE fuel-list concatenation), yielding uneven Ask handoff clarity.
Status: Unresolved

### Finding F1-06
Category: Improvement
Location: frontend/src/App.tsx — Component `App`, browse routes
Description:
routing is straightforward, but there is no route-level affordance for dataset release/version visibility; this mirrors backend/API opacity and leaves terminology (“release/version”) absent from browse UX.
Status: Unresolved

#### Nice-to-have
### Finding F1-07
Category: Nice-to-have
Location: N/A
Description:
`frontend/src/features/browse-lawa/LawaBrowsePage.tsx` and `frontend/src/features/browse-mbie/MbieBrowsePage.tsx` — both pages maintain independent inline filter controls and status footers; patterns are similar enough to be future shared-component candidates for consistency.
Status: Unresolved

### Finding F1-08
Category: Nice-to-have
Location: frontend/src/features/browse-lawa/RegionContextPanel.tsx
Description:
Component `RegionContextPanel` adds helpful context/disclaimer and improves LAWA page narrative polish.
Status: Unresolved

### Finding F1-09
Category: Nice-to-have
Location: frontend/src/App.tsx
Description:
file still contains trailing comment `// Test change`, which is minor polish debt in app routing surface.
Status: Unresolved

#### Questions
### Finding F1-10
Category: Question
Location: N/A
Description:
`frontend/src/features/browse-lawa/LawaBrowsePage.tsx` and `frontend/src/features/browse-mbie/MbieBrowsePage.tsx` — should both browse pages adopt the same default data-loading strategy (load-on-open vs require at least one filter) for consistency?
Status: Unresolved

### Finding F1-11
Category: Question
Location: frontend/src/features/browse-mbie/MbieBrowsePage.tsx
Description:
should chart tab explicitly mirror table-tab loading/error/empty messaging behavior?
Status: Unresolved

### Finding F1-12
Category: Question
Location: frontend/src/features/browse-lawa/LawaBrowsePage.tsx
Description:
for state table, should `p95` always render as a stable column (with placeholder when absent) to preserve table integrity?
Status: Unresolved

### Batch F2 — Shared Navigation / Layout / Dataset Discoverability

#### Critical
### Finding F2-01
Category: Critical
Location: frontend/src/components/NavBar.tsx — Component `NavBar`
Description:
mobile navigation has no alternate menu when desktop nav is hidden (`hidden sm:flex`), which materially reduces LAWA/MBIE page discoverability on small screens.
Status: Unresolved

#### Improvement
### Finding F2-02
Category: Improvement
Location: frontend/src/features/home/HomePage.tsx — Component `HomePage`
Description:
dataset entry points are bundled under generic “Explore Data” copy with equal styling, but do not communicate key semantic differences (MBIE annual/quarterly vs LAWA state/trend), reducing terminology clarity before users land on pages.
Status: Unresolved

### Finding F2-03
Category: Improvement
Location: frontend/src/components/Layout.tsx — Component `Layout`
Description:
fixed `max-w-4xl` wrapper is shared across all pages; works functionally but constrains dense dataset-table pages and contributes to inconsistent horizontal-scrolling affordances vs chart sections.
Status: Unresolved

### Finding F2-04
Category: Improvement
Location: frontend/src/features/home/HomePage.tsx
Description:
Component `HomePage`, CTA classes for MBIE/LAWA links include both `block` and `inline-flex` plus separate spacing patterns, indicating minor copy/paste style drift.
Status: Unresolved

### Finding F2-05
Category: Improvement
Location: frontend/src/components/NavBar.tsx
Description:
Component `NavBar`, nav labels (`MBIE Data`, `LAWA Data`) are clear, but there is no explicit active-context hint for sub-modes inside pages (e.g., annual vs quarterly, state vs trend), so users must infer current data mode from page internals.
Status: Unresolved

#### Nice-to-have
### Finding F2-06
Category: Nice-to-have
Location: N/A
Description:
`frontend/src/components/NavBar.tsx` and `frontend/src/features/home/HomePage.tsx` — top-level navigation/discoverability between Ask and dataset pages is straightforward on desktop and generally coherent.
Status: Unresolved

### Finding F2-07
Category: Nice-to-have
Location: frontend/src/components/Layout.tsx
Description:
shared shell provides consistent visual framing and prevents major cross-page style fragmentation.
Status: Unresolved

#### Questions
### Finding F2-08
Category: Question
Location: frontend/src/components/NavBar.tsx
Description:
should mobile navigation parity be treated as in-scope polish for Phase 15 portfolio credibility?
Status: Unresolved

### Finding F2-09
Category: Question
Location: frontend/src/features/home/HomePage.tsx
Description:
should home-page browse CTAs include one-line semantic qualifiers (e.g., MBIE annual/quarterly, LAWA state/trend) for terminology alignment?
Status: Unresolved

### Finding F2-10
Category: Question
Location: frontend/src/components/Layout.tsx
Description:
should browse pages use a wider max-width than Ask/Home to improve table/chart usability, or keep one global width policy?
Status: Unresolved

### Batch F3 — Shared UI Primitives + Shared Browse Styles

#### Critical
### Finding F3-01
Category: Critical
Location: N/A
Description:
None.
Status: Unresolved

#### Improvement
### Finding F3-02
Category: Improvement
Location: N/A
Description:
`frontend/src/features/browse-mbie/MbieBrowsePage.tsx` and `frontend/src/features/browse-lawa/LawaBrowsePage.tsx` with `frontend/src/components/ui/Button.tsx` and `frontend/src/index.css` — browse pages mix raw class buttons (`btn-primary`) and UI `<Button>` component usage, creating inconsistent interaction states/focus styles between MBIE and LAWA surfaces.
Status: Unresolved

### Finding F3-03
Category: Improvement
Location: N/A
Description:
`frontend/src/features/browse-mbie/MbieBrowsePage.tsx` and `frontend/src/features/browse-lawa/LawaBrowsePage.tsx` with `frontend/src/components/ui/Card.tsx` and `frontend/src/index.css` — pages mix `Card` component and plain `div.card/table-container` styles, producing subtle spacing/border divergence across otherwise similar sections.
Status: Unresolved

### Finding F3-04
Category: Improvement
Location: frontend/src/index.css
Description:
shared utility classes (`table-*`, `btn-*`, `card`) are global and untyped; they support fast iteration but make consistency drift easier than strictly componentized primitives.
Status: Unresolved

#### Nice-to-have
### Finding F3-05
Category: Nice-to-have
Location: N/A
Description:
`frontend/src/components/ui/Button.tsx` and `frontend/src/components/ui/Card.tsx` — primitives are clean and reusable, and can support improved cross-page cohesion if adopted consistently.
Status: Unresolved

### Finding F3-06
Category: Nice-to-have
Location: frontend/src/index.css
Description:
shared table utility classes provide a common baseline for both dataset pages.
Status: Unresolved

#### Questions
### Finding F3-07
Category: Question
Location: N/A
Description:
`frontend/src/features/browse-mbie/MbieBrowsePage.tsx` and `frontend/src/features/browse-lawa/LawaBrowsePage.tsx` — should dataset pages standardize on UI primitives (`Button`/`Card`) as default over ad-hoc utility classes for Phase 15 polish?
Status: Unresolved

### Finding F3-08
Category: Question
Location: frontend/src/index.css
Description:
do you want to keep global `btn-*`/`card` utility classes as a parallel path, or gradually consolidate to component-level styling contracts?
Status: Unresolved

Slice F discovery complete. No more files will be pulled into Slice F unless a later synthesis pass reveals a direct dataset-page dependency.

## Phase 15 Coverage Audit

Potentially uncovered areas (outside slices A–F):

### Finding F3-09
Category: Question
Location: N/A
Description:
`backend/src/main/java/nz/waiwatts/config/GlobalExceptionHandler.java`, `backend/src/main/java/nz/waiwatts/config/WebMvcConfig.java`, `backend/src/main/java/nz/waiwatts/config/RequestCorrelationFilter.java`, `backend/src/main/java/nz/waiwatts/config/RequestLoggingFilter.java`, `backend/src/main/java/nz/waiwatts/config/CorsLoggingFilter.java`.
Status: Unresolved

  Justification: These cross-cutting components can materially change public API error envelopes, CORS behavior, and request/response handling, which directly affects refusal-vs-error UX, determinism of API-facing behavior, and operational clarity.

### Finding F3-10
Category: Question
Location: N/A
Description:
`backend/src/main/java/nz/waiwatts/api/context/RegionContextController.java` and `backend/src/main/java/nz/waiwatts/service/context/RegionContextAggregationServiceImpl.java` (plus context DTOs).
Status: Unresolved

  Justification: Region context is surfaced in dataset UI (`RegionContextPanel`) and can influence dataset semantics presented to users; backend aggregation rules were not reviewed in A–F.

### Finding F3-11
Category: Question
Location: N/A
Description:
`backend/src/main/java/nz/waiwatts/api/datasets/DatasetCatalogController.java` and `backend/src/main/java/nz/waiwatts/service/datasets/DatasetCatalogServiceImpl.java`.
Status: Unresolved

  Justification: Dataset catalog endpoints can shape dataset_source/release discoverability semantics and public API contracts, but were not part of Slice B or D deep review.

### Finding F3-12
Category: Question
Location: N/A
Description:
`backend/src/main/resources/logback-spring.xml`.
Status: Unresolved

  Justification: Logging configuration is cross-cutting and can affect diagnosability of determinism/refusal/error behavior in production-like environments.

Coverage conclusion:

- A–F sufficiently cover the primary Phase 15 architecture scope (Ask pipeline, dataset read APIs, ingestion, persistence/migrations, Ask UX, dataset-page UX).
- The main remaining gap is cross-cutting infrastructure and catalog/context endpoints listed above; these are bounded follow-up areas rather than a missing core slice.
