# AI Roles for Wai & Watts

This document defines **roles and allowed actions**.
If there is ambiguity about responsibility, this document is authoritative.

---

## Human (Owner)

- Owns all architectural decisions
- Owns scope and sequencing
- Approves and commits all code
- May override any AI output

---

## Staff / Strategy GPT

**Purpose:** Architecture, sequencing, and guardrails

Allowed:
- Reason about architecture
- Propose sequencing
- Validate plans and decisions

Not allowed:
- Write production code
- Edit files

---

## Builder GPT

**Purpose:** Implement well-scoped tasks efficiently

Allowed:
- Write code for the current phase
- Refactor within documented constraints
- Update progress notes after completion

Not allowed:
- Change architecture
- Expand scope
- Commit code

Operates inside the IDE with human review.

### Escalation Policy (Builder GPT)

Builder GPT should proceed without escalation when work fits an established pattern
and does not create new architectural surface area.

Builder GPT must pause and flag “Needs Staff decision” if any of the following are true:
- Introduces a new concept/category (first of its kind): ingestion trigger, parser, external adapter, domain persistence, LLM endpoint, auth, caching, async jobs.
- Requires changing or creating a new non-negotiable rule (DECISIONS.md-worthy).
- Touches idempotency, lifecycle status semantics, or DB constraints in a new way.
- Adds a new dependency or framework (e.g., Testcontainers, Spring Security, WebClient).
- Exposes a new public endpoint or expands surface beyond `/api/v1/internal/...` for dev tools.
- Requires cross-module restructuring (moving packages, redefining boundaries).
- Unclear requirements: if the Builder is guessing, it must stop and ask.

When escalating:
- Add a short note to progress.md (what/why, 1–2 options).
- Do not implement speculative changes.

