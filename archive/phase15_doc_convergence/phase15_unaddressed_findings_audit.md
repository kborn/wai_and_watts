# Phase 15 Unaddressed Findings Audit

Date: 2026-02-20  
Scope: Unaddressed actionable findings from `phase15_architectural_review_findings.md`, validated against current code state.

---

## Critical — Ask Pipeline Determinism and Safety

### 1) `/ask` still leaks runtime failures as HTTP 500 (no deterministic refusal envelope)
- Evidence:
  - `backend/src/main/java/nz/waiwatts/explanations/api/ExplanationController.java:254` (`catch (Exception)` logs then rethrows)
  - `backend/src/main/java/nz/waiwatts/explanations/service/RequestValidationService.java:139` (`(Integer)` cast before type validation can throw)
  - `backend/src/main/java/nz/waiwatts/config/GlobalExceptionHandler.java:17` (generic 500 map response, not `/ask` refusal shape)
- Related findings: A1-02, A2-01, A1-09

### 2) MBIE dataset selection remains candidate-order dependent
- Evidence:
  - `backend/src/main/java/nz/waiwatts/explanations/service/DatasetSelectionService.java:109` (iterates candidates in returned order; first verified wins)
- Related findings: A2-02, A2-08

### 3) LAWA indicator-only filtering still bypassed when region is absent
- Evidence:
  - `backend/src/main/java/nz/waiwatts/explanations/builder/LawaStateMultiYearFactPackBuilder.java:146` (`indicatorForQuery = regionFilter == null ? null : indicatorFilter`)
  - `backend/src/main/java/nz/waiwatts/explanations/builder/LawaTrendMultiYearFactPackBuilder.java:154` (same pattern)
- Related findings: A4-01, A4-02, A4-12, A4-13

### 4) Stub provider still produces numeric claims with classification-only citations
- Evidence:
  - `backend/src/main/java/nz/waiwatts/explanations/provider/StubExplanationProvider.java:307` (injects numeric metric values into explanation text)
  - `backend/src/main/java/nz/waiwatts/explanations/provider/StubExplanationProvider.java:320` (citations limited to classification IDs)
- Related findings: A3-01, A3-10

---

## Improvement — Determinism / Contract / Test Coverage Gaps

### 5) Citation mapping output is not stabilized (no dedupe/sort)
- Evidence:
  - `backend/src/main/java/nz/waiwatts/explanations/service/CitationMapper.java:16` (maps in provider order, appends directly)
- Related findings: A3-03, A3-09

### 6) `/ask` integration test still mocks core pipeline services
- Evidence:
  - `backend/src/test/java/nz/waiwatts/explanations/api/NaturalLanguageEndpointIntegrationTest.java:44`
  - Mocked beans include `ExplanationService`, `IntentParserService`, `RequestValidationService`, `DatasetSelectionService`
- Related findings: A5-01, A5-08

### 7) Tests still codify exception propagation for builder/provider runtime errors
- Evidence:
  - `backend/src/test/java/nz/waiwatts/explanations/service/ExplanationServiceImplEdgeCaseTest.java:198`
  - `backend/src/test/java/nz/waiwatts/explanations/service/ExplanationServiceImplEdgeCaseTest.java:219`
- Related findings: A5-02, A5-09

### 8) FactPack timestamp remains nondeterministic (`now()`)
- Evidence:
  - `backend/src/main/java/nz/waiwatts/explanations/dto/FactPack.java:25`
- Related findings: A6-01, A6-07

### 9) OpenAI provider parse path does not enforce non-empty explanation text for non-refusal
- Evidence:
  - `backend/src/main/java/nz/waiwatts/explanations/provider/OpenAiExplanationProvider.java:74` (JSON parse accepted as-is)
- Related findings: A3-04

### 10) LAWA trend `units` cleanup is partial (dead field still in parsed record path)
- Evidence:
  - `backend/src/main/java/nz/waiwatts/ingestion/lawa/LawaTrendMultiYearParsedRecord.java:13`
  - `backend/src/main/java/nz/waiwatts/ingestion/lawa/LawaTrendMultiYearIngestion.java:83`
- Note:
  - DB/domain removal exists (`backend/src/main/resources/db/migration/V15__remove_lawa_trend_units.sql:1`, `backend/src/main/java/nz/waiwatts/domain/lawa/LawaTrendMultiYearRecord.java:1`), but ingestion-side model still carries `units`.
- Related findings: Step 2.2 hygiene intent (partial)

---

## Context — Already Addressed (for contrast)

- Deterministic/fail-fast builder selection:
  - `backend/src/main/java/nz/waiwatts/explanations/service/ExplanationServiceImpl.java:291`
- `findAll()` removed from ask builders; repository-scoped reads:
  - `backend/src/main/java/nz/waiwatts/explanations/builder/MbieGenerationAnnualFactPackBuilder.java:132`
  - `backend/src/main/java/nz/waiwatts/explanations/builder/MbieGenerationQuarterlyFactPackBuilder.java:139`
- Canonical release pinning in fact-pack builders:
  - `backend/src/main/java/nz/waiwatts/explanations/builder/MbieGenerationAnnualFactPackBuilder.java:135`
  - `backend/src/main/java/nz/waiwatts/explanations/builder/MbieGenerationQuarterlyFactPackBuilder.java:142`
  - `backend/src/main/java/nz/waiwatts/explanations/builder/LawaStateMultiYearFactPackBuilder.java:150`
  - `backend/src/main/java/nz/waiwatts/explanations/builder/LawaTrendMultiYearFactPackBuilder.java:158`
- Region normalization at persistence boundary:
  - `backend/src/main/java/nz/waiwatts/ingestion/lawa/LawaStateMultiYearIngestion.java:221`
  - `backend/src/main/java/nz/waiwatts/ingestion/lawa/LawaTrendMultiYearIngestion.java:207`
- Coverage baseline enforcement:
  - `backend/pom.xml:84`
  - `.github/workflows/ci.yml:25`
- Abstract CSV parser introduced:
  - `backend/src/main/java/nz/waiwatts/ingestion/util/AbstractCsvParser.java:1`
