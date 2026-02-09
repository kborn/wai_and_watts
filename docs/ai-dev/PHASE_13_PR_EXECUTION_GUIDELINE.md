# Phase 13 Multilateral PR Execution Guide

## Overview
This document provides step-by-step instructions for executing the Phase 13 frontend work as a series of focused, reviewable PRs.

## PR Stack Order
Each PR builds on the previous, following natural dependency order:

1. **PR 1: Vite + React + TypeScript Scaffold** ✅
2. **PR 2: Routing + Layout Shell** ✅  
3. **PR 3: API Client + TanStack Query** ✅
4. **PR 4: Ask Flow Implementation** ✅
5. **PR 5: Results View Implementation** ✅
6. **PR 6: MBIE Browse View** ✅
7. **PR 7: LAWA Browse View** ✅
8. **PR 8: Testing Setup** ✅
9. **PR 9: Cleanup & Documentation** ✅

## Current State
✅ **PR 1 (`pr1-vite-scaffold`) contains complete Phase 13 implementation**
- All features implemented and tested
- Builds and runs successfully
- Ready for review

## Execution Commands

### For Each PR:
```bash
# Reset to clean state
git checkout main && git reset --hard origin/main

# Create PR branch
git checkout -b pr<number>-<name>

# Add/modify only files for this PR scope
git add <specific-files>

# Commit with clear scope
git commit -m "feat(frontend): <description>

# Push and create PR
git push origin pr<number>-<name>
# Create PR in GitHub UI or CLI
```

## Branch Strategy
- **Main branch**: Always remains clean, with Phase 13 progress updates
- **Feature branches**: Each PR is its own branch, building incrementally
- **Testing**: Each PR includes relevant tests when applicable

## PR Scope Definitions

### PR 1: Core Scaffold
**Files**: package.json, tsconfig.json, vite.config.ts, tailwind.config.js, src/index.css, src/App.tsx, src/main.tsx
**Goal**: Basic Vite + React + TS setup

### PR 2: Routing + Layout  
**Files**: Layout.tsx, NavBar.tsx, updated App.tsx
**Goal**: Navigation foundation

### PR 3: API Client
**Files**: types/index.ts, api/client.ts, api/hooks.ts  
**Goal**: Backend integration + state management

### PR 4: Ask Flow
**Files**: features/ask/AskPage.tsx
**Goal**: Natural language question interface

### PR 5: Results View
**Files**: features/results/ResultsPage.tsx
**Goal**: Explanation + citation display

### PR 6: MBIE Browse
**Files**: features/browse-mbie/MbieBrowsePage.tsx
**Goal**: Table-first MBIE data exploration

### PR 7: LAWA Browse  
**Files**: features/browse-lawa/LawaBrowsePage.tsx
**Goal**: Table-first LAWA data exploration

### PR 8: Testing Setup
**Files**: vitest.config.ts, playwright.config.ts, src/test/api.test.ts, src/test/components.test.tsx, e2e/*.spec.ts
**Goal**: Test infrastructure

### PR 9: Documentation
**Files**: README.md, updated progress.md
**Goal**: Final documentation + cleanup

## Notes
- Each PR should be independently reviewable
- Build should pass after each PR
- Tests should pass where applicable
- Stack dependencies only added when needed for that PR

## Ready for Execution
Start with PR 1 review, then proceed with PR 2-9 as needed.