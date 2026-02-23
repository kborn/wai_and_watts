# AI Governance Case Study

## The problem
AI can accelerate implementation, but without guardrails it increases risk of architectural drift, invariant violations, and ungrounded LLM outputs.

## The approach
Wai & Watts enforced governance patterns that mirror production teams:
- Role separation (architecture vs implementation)
- Explicit escalation policy for new architectural surface area
- Append‑only decision logging
- Fixture‑first development discipline for new datasets
- Fact Packs as the exclusive boundary for any LLM interaction
- Natural language “ask” constrained to routing/intent parsing only
- Deterministic refusal for unsupported or ambiguous requests
- No autonomous AI commits

## Why this is valuable
The value is not “AI wrote code.” The value is the *control system* that kept AI aligned with architectural intent.

## Decision Summary

| Decision | Why | Outcome | Evidence |
|---|---|---|---|
| Human-owned architecture authority | Prevent non-deterministic architectural drift | Stable contracts across phases and sessions | `engineering/project-context.md`, `engineering/roles.md` |
| Append-only decision logging | Keep rationale traceable during iterative AI sessions | Historical continuity despite session resets | `engineering/decisions.md` |
| Contract-first ingestion | Ensure deterministic parsing and schema safety | Reusable ingestion lifecycle across MBIE + LAWA | `engineering/design/001-architecture.md`, `engineering/design/002-contracts.md` |
| Fact Pack boundary for LLMs | Prevent ungrounded DB access and hallucinated claims | Grounded explanations with citation/refusal contract | `engineering/design/009-fact-pack-contract.md`, `docs/05-llm-safety-model.md` |
| Archive-first documentation policy | Preserve rigor trail without polluting canonical docs | Clean reviewer-facing docs plus full historical traceability | `docs/06-documentation-governance.md`, `archive/` |
