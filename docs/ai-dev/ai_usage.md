# Wai & Watts — AI Usage

This document explains **how AI tools are used intentionally**.

---

## Principles
- AI accelerates execution
- Humans retain judgment
- No autonomous code commits

---

## Roles in Practice

### Staff / Strategy GPT
Used for:
- Architecture validation
- Sequencing decisions

Not used for:
- Writing code

---

### Builder GPT
Used for:
- Implementing scoped tasks

Constraints:
- Must follow progress.md and decisions.md

---

## Session recovery quickstart
- Read `project-context.md` → understand guardrails and document authority
- Read `progress.md` → identify the active phase and Definition of Done
- Read `decisions.md` → understand non-negotiables and rationale
- Confirm the next action with the human before expanding scope

## Maintenance workflow for Builder GPT
- After completing a task:
  - Update `progress.md` (status, brief notes, links to commits/PRs)
  - Append any new decisions to `decisions.md` (append-only, with rationale)
