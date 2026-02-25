# Change Risk Checklist

Use this checklist before merging material changes to `main`.

## 1) Data and schema risk
- [ ] Forward-only Flyway migration added (if schema changed).
- [ ] Backward compatibility considered for existing seeded data.
- [ ] Ingestion idempotency behavior revalidated (no duplicate release creation).

## 2) API and contract risk
- [ ] Public API behavior changes are documented in canonical docs.
- [ ] DTO shapes and refusal envelopes remain deterministic.
- [ ] Capabilities/catalog outputs still match supported paths.

## 3) LLM safety and grounding risk
- [ ] LLM boundary unchanged (no DB/domain access from provider path).
- [ ] Non-refusal responses still require citations.
- [ ] Citations still map to Fact Pack fact IDs for the request.

## 4) CI and test risk
- [ ] Relevant workflows were triggered and passed.
- [ ] Contract tests (`-Dgroups=contract`) passed.
- [ ] Frontend unit tests passed for UI/API-client changes.
- [ ] E2E smoke coverage passed for user-visible flow changes.

## 5) Documentation and governance risk
- [ ] `README.md` operational commands still run as written.
- [ ] Canonical docs updated in the same change set as code behavior changes.
- [ ] Historical/legacy notes moved to `archive/` instead of mixed into canonical docs.
- [ ] No forbidden legacy canonical references were introduced.

## 6) Rollback/readiness check
- [ ] Clear rollback path identified (migration rollback strategy or compensating forward fix).
- [ ] Known limitations or deferred follow-ups captured in `engineering/progress.md`.
