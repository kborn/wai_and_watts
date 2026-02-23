# Explanation Service Architecture — Phase 11

Status: Draft v1  
Owner: Staff  
Consumers: Builder Implementation

---

## Architecture Overview

User Question
→ Explanation Service
→ Fact Pack Builder(s)
→ Fact Pack JSON
→ LLM Provider Adapter
→ Explanation Response + Citations

---

## Core Components

### Explanation Service
Responsibilities:
- Accept explanation request
- Select appropriate Fact Pack Builder
- Generate Fact Pack
- Call LLM Adapter
- Validate citation presence

Must NOT:
- Query DB directly
- Contain dataset-specific logic

---

### Fact Pack Builders (Dataset Specific)

Required:
- MBIE Annual
- MBIE Quarterly
- LAWA State
- LAWA Trend

Rules:
- Deterministic output
- DB query only
- No LLM calls
- Include provenance

---

### LLM Provider Adapter

Responsibilities:
- Serialize prompt + fact pack
- Call provider
- Return structured response

Must NOT:
- Query DB
- Modify facts

---

## Refusal Enforcement

If:
- Missing facts
- Unsupported question class
- Disallowed claim

Return deterministic refusal message.

---

## Testing Strategy

Required:
- Fact pack determinism tests
- Refusal trigger tests
- Citation presence validation (best effort)
