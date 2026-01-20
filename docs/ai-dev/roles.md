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
