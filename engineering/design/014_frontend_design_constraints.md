# Phase 13 --- SE Design Constraints

## Stack (Locked)

React, TypeScript, Vite, React Router, TanStack Query, Tailwind, Vitest,
Playwright

## Rendering Model

SPA only. No SSR.

## State Philosophy

Server State → TanStack Query UI State → React local state

No global state store in Phase 13.

## Guardrails

Frontend MUST NOT: - Compute metrics - Interpret dataset semantics -
Perform intent parsing - Construct fact packs - Perform explanation
logic

Frontend MAY: - Format requests - Render responses - Provide UX
affordances

## Testing

Vitest for unit tests. Playwright smoke tests required in Phase 13.
