# Phase 15 Second-Round Verification Findings

Date: 2026-02-21
Scope:
- `docs/phase-notes/phase15/phase15_architectural_review_findings.md`
- `docs/phase-notes/phase15/phase15_architectural_remediation_plan.md`
- `docs/phase-notes/phase15/phase15_refactoring_execution_checklist.md`
- `docs/phase-notes/phase15/phase15_unaddressed_findings_audit.md`

Method:
- Re-validated each previously unaddressed audit item against current code.
- Verified critical paths with targeted backend test runs.
- Spot-checked Phase 15 checklist deliverables in backend/frontend/docs.

---

## Findings (Ordered by Severity)

### 1) Follow-up remediated: year-filter type safety before validation contract
Category: Improvement (contract correctness / refusal precision)

Status: **Resolved (patched 2026-02-21)**

Evidence:
- `backend/src/main/java/nz/waiwatts/explanations/service/RequestValidationService.java:139`
  - `Integer startYear = (Integer) filters.get("startYear");`
- `backend/src/main/java/nz/waiwatts/explanations/service/RequestValidationService.java:140`
  - `Integer endYear = (Integer) filters.get("endYear");`
- Type checks occur later at `backend/src/main/java/nz/waiwatts/explanations/service/RequestValidationService.java:152-156`.

Remediation:
- Reordered `validateFilters` to run type checks before year-range comparison/casts.
- Added regression tests:
  - `backend/src/test/java/nz/waiwatts/explanations/service/RequestValidationServiceTest.java` (`failsWhenStartYearIsNotInteger`, `failsWhenEndYearIsNotInteger`).

Verification:
- `mvn -f backend -Dtest=RequestValidationServiceTest,NaturalLanguageEndpointIntegrationTest test` passed (19 tests, 0 failures).

---

## Closure Status of Prior Unaddressed Audit Items

1. `/ask` deterministic refusal envelope for runtime failures: **Closed**
- `backend/src/main/java/nz/waiwatts/explanations/api/ExplanationController.java:258-274` now catches exceptions and returns refusal payload.

2. MBIE dataset selection candidate-order dependence: **Closed**
- Deterministic tie-breaker added at `backend/src/main/java/nz/waiwatts/explanations/service/DatasetSelectionService.java:374-404`.
- Covered by tests in `backend/src/test/java/nz/waiwatts/explanations/service/DatasetSelectionServiceTest.java:205-278`.

3. LAWA indicator-only filtering bypass without region: **Closed**
- State builder passes independent indicator filter into repository query: `backend/src/main/java/nz/waiwatts/explanations/builder/LawaStateMultiYearFactPackBuilder.java:130-147`.
- Trend builder equivalent: `backend/src/main/java/nz/waiwatts/explanations/builder/LawaTrendMultiYearFactPackBuilder.java:130-155`.
- Repository predicates apply indicator independent of region:
  - `backend/src/main/java/nz/waiwatts/persistence/repositories/LawaStateMultiYearRecordRepository.java:19-21`
  - `backend/src/main/java/nz/waiwatts/persistence/repositories/LawaTrendMultiYearRecordRepository.java:33-36`

4. Stub provider numeric claims with classification-only citations: **Closed**
- Metrics used for numeric statements and included in citation list:
  - `backend/src/main/java/nz/waiwatts/explanations/provider/StubExplanationProvider.java:300-334`

5. Citation mapping output stabilization (dedupe/sort): **Closed**
- Stable canonicalization in mapper:
  - `backend/src/main/java/nz/waiwatts/explanations/service/CitationMapper.java:21-26`
- Test coverage:
  - `backend/src/test/java/nz/waiwatts/explanations/service/CitationMapperTest.java:16-30`

6. `/ask` integration test mocking core pipeline: **Closed**
- Real pipeline integration test class now runs with real services:
  - `backend/src/test/java/nz/waiwatts/explanations/api/NaturalLanguageEndpointIntegrationTest.java:21-55`

7. Tests codifying exception propagation from builder/provider errors: **Closed**
- Tests now assert refusal behavior (no propagation):
  - `backend/src/test/java/nz/waiwatts/explanations/service/ExplanationServiceImplEdgeCaseTest.java:188-223`

8. FactPack nondeterministic timestamp default (`now()`): **Closed**
- Default timestamp unset for deterministic construction:
  - `backend/src/main/java/nz/waiwatts/explanations/dto/FactPack.java:24-29`

9. OpenAI parse path accepting non-refusal with empty explanation text: **Closed**
- Non-refusal payloads now enforce non-empty explanation text:
  - `backend/src/main/java/nz/waiwatts/explanations/provider/OpenAiExplanationProvider.java:81-89`
- Test coverage:
  - `backend/src/test/java/nz/waiwatts/explanations/provider/OpenAiExplanationProviderTest.java:61-77`

10. LAWA trend `units` cleanup partial in ingestion parsed model: **Closed**
- `units` removed from trend parsed record:
  - `backend/src/main/java/nz/waiwatts/ingestion/lawa/LawaTrendMultiYearParsedRecord.java:6-21`
- Ingestion no longer sets trend `units`:
  - `backend/src/main/java/nz/waiwatts/ingestion/lawa/LawaTrendMultiYearIngestion.java:83-101`

---

## Checklist Spot-Check (Phase 15 Refactoring Execution Checklist)

Validated as implemented in current repo state:
- Step 1: deterministic builder selection, release pinning, citation stabilization, refusal envelope hardening.
- Step 2: region normalization, trend `units` removal, abstract CSV parser adoption.
- Step 3: API versioning and `dataset_release` semantics documented in `README.md`.
- Step 4: JaCoCo threshold + CI enforcement (`backend/pom.xml`, `.github/workflows/ci.yml`).
- Step 5: refusal UX differentiation present in `frontend/src/features/results/ResultsPage.tsx`.
- Step 6: README scope/non-goals/evolution narrative and `DEMO.md` present.

---

## Test Execution

Executed:
- `mvn -f backend -Dtest=NaturalLanguageEndpointIntegrationTest,ExplanationServiceImplEdgeCaseTest,DatasetSelectionServiceTest,RequestValidationServiceTest,OpenAiExplanationProviderTest,LawaTrendMultiYearCsvParserTest test`

Result:
- **BUILD SUCCESS**
- 51 tests run, 0 failures, 0 errors.

Note:
- JaCoCo emitted JDK instrumentation warnings during test run, but tests completed successfully.

---

## Final Closure Addendum (2026-02-21)

Follow-up review identified one residual taxonomy drift in frontend refusal-code handling:
- `frontend/src/features/results/ResultsPage.tsx` accepted both `CAPABILITY_UNSUPPORTED` and `UNSUPPORTED_CAPABILITY`.
- `frontend/src/test/components.test.tsx` still used `CAPABILITY_UNSUPPORTED`.

Remediation applied:
- Canonicalized frontend and test fixture to `UNSUPPORTED_CAPABILITY` only.

Closure:
- All previously reported actionable Phase 15 review findings and follow-up audit items are now closed.
- Phase 15 architectural remediation execution is complete.
