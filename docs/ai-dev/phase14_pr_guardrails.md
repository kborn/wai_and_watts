# Phase 14 — PR Guardrails (UI/UX Styling) — Option B (Modern SaaS Feel)

Date: 2026-02-12

This document defines **what is allowed in each PR** during Phase 14 so we improve polish without scope creep.
Phase 14 is intentionally iterative, but changes must stay within defined boundaries.

## Global Phase 14 Rules (Apply to Every PR)

### Non-negotiables
- **No new backend endpoints** unless explicitly requested by Staff.
- **No changes to domain logic** (backend remains authoritative).
- **No changes to Explanation intent/refusal rules**.
- **No new global frontend state store** (no Redux/Zustand/MobX).
- **No SSR / framework re-platforming** (React + Vite stays).
- **No large refactors** unrelated to UI/UX polish.

### Chart constraint
- Charts are **presentational only**.
- Use **existing returned data**; do not invent new metrics.
- Basic grouping/aggregation for display is allowed **only if it is a direct re-expression of returned rows** (e.g., sum by year across currently displayed rows).

### “Modern SaaS feel” style direction (Option B)
- Clean surfaces, generous spacing, subtle borders, soft shadows.
- Clear hierarchy: page title → filters/actions → primary content.
- UI should feel like a modern product (e.g., Linear/Vercel vibe) without becoming a design-system project.

### Review checklist (all PRs)
- [ ] No contract changes to backend APIs
- [ ] No new endpoints
- [ ] No domain logic moved into frontend
- [ ] Visual improvements are consistent across pages touched
- [ ] Screenshots updated only if visuals materially changed (optional)

---

## PR Plan + Guardrails

### PR 14.1 — Styling Foundation + Layout Shell
**Goal:** Establish consistent layout + typography + spacing so every page benefits.

Allowed:
- Tailwind base styles, layout container, max-width, padding/gutters
- Typography scale (H1/H2/body/caption)
- App shell: header/nav, active route styling
- Global background, card surfaces, border/shadow primitives

Not allowed:
- New features or routes
- Chart work
- Data fetching changes beyond superficial wiring fixes

Deliverables:
- Consistent page container
- Header/nav looks intentional and modern
- At least one page (Ask) updated to validate the system

---

### PR 14.2 — Core Components (Minimal, Reusable)
**Goal:** Create a small set of reusable components to drive consistency.

Allowed components (keep minimal):
- Button (primary/secondary/ghost)
- Card
- Callout (error/refusal/info)
- Input + Select styling wrappers
- Table styling wrapper (header, row, empty state)

Not allowed:
- Full component library buildout
- Storybook
- Deep theming systems beyond what’s needed

Deliverables:
- Components used in at least 2 pages
- Focus/hover/disabled states included

---

### PR 14.3 — Ask + Results Polish
**Goal:** Make the primary demo flow feel excellent.

Allowed:
- Improve Ask layout, examples, action placement
- Improve Results readability, citations section, refusal card
- Add loading skeleton/spinner
- Add dev-only diagnostics (if already planned)

Not allowed:
- New backend logic
- New explanation modes
- New routing patterns

Deliverables:
- Ask → Results experience feels “product-grade”
- Refusal view visually distinct and informative

---

### PR 14.4 — Browse Pages Polish (MBIE + LAWA)
**Goal:** Make filters, tables, and empty/error states look solid.

Allowed:
- Filter bar layout (sticky optional)
- Table styling (density, zebra, header, empty state)
- Clear “Explain this” CTA placement
- Better loading and error experiences

Not allowed:
- Introducing heavy visualization frameworks in this PR
- Reworking API contracts

Deliverables:
- MBIE and LAWA pages feel cohesive with Ask/Results
- Filters look modern and consistent

---

### PR 14.5 — Charts (Presentational, Filter-Driven)
**Goal:** Add a modern chart above tables that responds to selected filters.

Allowed:
- Choose a lightweight chart lib (see Staff constraints elsewhere) **or** use a simple approach agreed by Staff
- Build a single chart component reused across MBIE/LAWA if possible
- Chart uses currently displayed dataset rows; minimal aggregation for display is ok

Not allowed:
- New backend endpoints for chart-specific aggregation
- “Dashboard” buildout
- Multi-chart pages

Deliverables:
- MBIE: chart updates with annual/quarterly toggle + filters
- LAWA: chart updates with state/trend toggle + filters
- Table remains present and primary

---

### PR 14.6 — Responsive Pass + Final Consistency Sweep
**Goal:** Ensure it’s not broken on tablet/mobile and polish rough edges.

Allowed:
- Responsive layout tweaks
- Spacing fixes, truncation handling, overflow fixes
- Small copy improvements

Not allowed:
- Net-new features

Deliverables:
- Works on common widths (desktop/tablet/mobile)
- No obvious broken layouts

---

## What to do if scope questions arise
If a change would violate PR guardrails, tag it as:
> **Needs Staff Decision**

and do not implement until clarified.
