# Phase 11 — Grounded LLM Explanations (Spec)

Status: Draft v1  
Phase: 11  
Owners: PM + Staff  
Implementer: Builder (after acceptance)

---

## Phase Goal

Enable Wai & Watts to produce human-readable explanations of environmental data trends using LLMs while guaranteeing outputs are grounded in persisted database facts.

The LLM is an explanation renderer, not a reasoning engine, data source, or decision maker.

---

## Supported Question Classes

### Descriptive
- Explain hydro generation trends between years
- Describe water quality state changes

### Comparative
- Compare wind vs hydro generation

### Contextual Narrative (Fact Bound)
- Explain what the trend means for the electricity generation mix

---

## Explicitly Unsupported

Must Refuse:
- Forecasting
- Causal claims
- Policy recommendations
- Hypothetical or counterfactual analysis

---

## Explanation Requirements

Every explanation must:
- Use only fact pack data
- Include citations to fact pack fields
- Avoid speculation
- Use neutral scientific tone
- Be understandable to non-technical users

---

## Phase 11 Engineering Deliverables

- Fact Pack Contract (design)
- Dataset-specific Fact Pack Builders
- Explanation Service
- LLM Provider Adapter
- Refusal + Citation enforcement tests

---

## Phase 11 Success Criteria

Engineering:
- Deterministic fact pack generation
- LLM never accesses DB or raw entities
- At least one refusal scenario tested

Product:
- At least 3 grounded explanation demos
- No hallucinated values in test corpus

Portfolio:
- Clear narrative: curated fact packs prevent hallucination
