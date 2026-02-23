# Wai & Watts — Phase 13 Product Slice (PM → SE) — Revised

## Phase 13 Context

Phase 13 introduces a thin but production-credible UI client over the existing backend.

Goals:
- Demonstrate natural language explanation workflow
- Demonstrate dataset exploration workflow
- Demonstrate grounded AI transparency (citations + refusal behavior)
- Establish a UI foundation that can grow without re-platforming

Non-Goals:
- Not building a full product UI
- Not building visualization-heavy dashboards
- Not moving domain meaning into frontend

The backend remains the source of truth for:
- Data semantics
- Fact pack construction
- Explanation safety
- Intent parsing correctness

Frontend is a client surface only.

Added clarity: The frontend must treat backend responses as authoritative and must not reinterpret explanation/citation meaning or derive new metrics client-side.

## 1️⃣ Phase 13 Product Slice

Core User Flows

Flow 1 — Ask a Question (Primary Flow)
User can:
1. Open Ask page
2. Enter natural language question
3. Submit
4. See one of:
   - Explanation + citations
   - Deterministic refusal

Nice-to-have:
- Example questions
- Optional parsed intent debug view (toggle or dev-only)

Flow 2 — View Explanation Results
User can:
- Read explanation text
- See structured citations
- Clearly distinguish refusal vs success
- Optionally copy/share question

Flow 3 — (Optional / Stretch) Dataset Exploration → Explain
User can:
1. Browse dataset slice (MBIE or LAWA)
2. Apply simple filters
3. Trigger “Explain this data” → explanation result view

Clarification: Dataset exploration is read-only in Phase 13 and should use existing read APIs; avoid introducing new aggregation/data-shaping backend endpoints unless required for the minimal experience.

## Demo Narrative

Story We Want to Tell in 5 Minutes
1️⃣ This is a real environmental data platform → Show dataset context or quick browse
2️⃣ You can ask real natural language questions → Ask a real MBIE or LAWA question
3️⃣ The system is grounded and safe → Show citations → Show refusal behavior example
4️⃣ This is real engineering, not prompt magic → Mention fact pack boundary concept (visually optional)

## Acceptance Criteria

Ask Workflow
- User can submit NL question
- Request hits NL endpoint
- Result view renders explanation or refusal
- Loading and error states handled cleanly

Results View
- Explanation clearly readable
- Citations clearly visible
- Refusal visually distinct from success
- Handles long responses gracefully

General UX
- Fast perceived performance
- Clean modern layout
- Mobile responsiveness is nice-to-have, not required

## Architecture Boundaries

Frontend MUST NOT:
- Compute metrics
- Interpret dataset semantics
- Construct or modify fact packs
- Perform intent parsing
- Implement refusal logic locally

Frontend MAY:
- Format requests
- Render responses
- Provide UX affordances (examples, filters, nav)

Added flexibility: Phase 13 intentionally allows UI implementation iteration and refinement as long as backend contracts and the above boundaries remain intact.

## “If We Only Ship X, We Still Look Good”

Absolute Minimum Demoable Product
Must include:
- Ask page
- Results view
- Explanation + citation rendering
- Refusal rendering
- Clean navigation shell

If this ships, Phase 13 is successful.

## Strong Demo Version (Still Reasonable Scope)

Add:
- Example questions
- Parsed intent debug toggle
- Basic dataset browse page

## 2️⃣ Phase 13 UX Scope

Likely Pages

Ask Page (Required)
Contains:
- Question input
- Submit action
- Example questions (optional)
- Lightweight explanation of what system does (optional)

Results View (Required)
Contains:
- Question echo
- Explanation content
- Citations section
- Refusal UI variant

Dataset Browse (Optional / Stretch)
Simple only:
- Dataset selector
- Minimal filters
- Table or simple list
- “Explain this” entry point

## UX Philosophy

Should feel: clean, modern, fast, real product surface, not overbuilt.

Avoid: dashboard explosion, complex visualization frameworks, heavy global state architecture early.

## 3️⃣ SE Decision Expectations (Early Phase 13)

SE should define:
- UI framework
- Rendering model (SPA / SSR / hybrid)
- API typing strategy
- State / data fetching pattern
- Project structure and layering

PM is intentionally not prescribing tooling — only requirements and constraints.

## 4️⃣ Phase 13 Success Definition

Phase 13 succeeds if a reviewer can quickly understand:
- What Wai & Watts does
- Why grounded AI matters
- That backend engineering is real and serious
- That frontend is modern and extensible
