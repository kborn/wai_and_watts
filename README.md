# wai_and_watts
Wai and Watts - Explore the impact of energy consumption on water quality in New Zealand

## Repository layout

Top-level modules:
- `backend/` — Spring Boot application (Java 21, Spring Boot 3.3)
- `frontend/` — (planned) thin web client

Backend source tree has moved under `backend/src/...`:
- `backend/src/main/java/nz/waiwatts/...`
- `backend/src/main/resources/...`

## Build & Test (local)

From repository root:
- Build + test backend: `mvn -f backend clean verify`
- Test only: `mvn -f backend clean test`

## Run (backend)

At this stage (Step 2), there are no controllers, entities, or ingestion code. The goal is only to boot the app and run Flyway V1.

Option A (cd into module):
- `cd backend`
- Start (dev profile active): `mvn spring-boot:run`

Option B (from root using -pl):
- `mvn -pl backend spring-boot:run`

Expected on startup:
- Flyway applies migrations (`V1..Vn`) successfully.
- Application starts without errors.

Config:
- Postgres via env vars: `DB_URL`, `DB_USER`, `DB_PASSWORD` (defaults provided in `application.yml`).

Note: The legacy `src/` directory at repo root is not built and can be safely deleted outside this environment.

---

## Pre-push guardrail (recommended)

Before pushing, run the test suite locally:

```
mvn -f backend clean verify
```

Optional: install a Git pre-push hook to prevent pushing broken builds:

```
cp scripts/git-hooks/pre-push.sample .git/hooks/pre-push
chmod +x .git/hooks/pre-push
```

This hook will run `mvn -f backend clean verify` and abort the push if the build/tests fail.

---

## Continuous Integration (GitHub Actions)

CI runs on every push and pull request. It builds the backend and runs all tests.

Badge:

![Backend CI](https://github.com/kborn/wai_and_watts/actions/workflows/ci.yml/badge.svg)

Workflow:
- Location: `.github/workflows/ci.yml`
- Java: Temurin JDK 21
- Build: `mvn -f backend -B clean verify`
