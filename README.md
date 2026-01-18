# wai_and_watts
Wai and Watts - Explore the impact of energy consumption on water quality in New Zealand

## Repository layout

Top-level modules:
- `backend/` — Spring Boot application (Java 21, Spring Boot 3.3)
- `frontend/` — (planned) thin web client

Backend source tree has moved under `backend/src/...`:
- `backend/src/main/java/nz/waiwatts/...`
- `backend/src/main/resources/...`

## Build

From repository root (aggregator):
- Build all modules: `mvn -DskipTests package`

## Run (backend) — Step 2 smoke test

At this stage (Step 2), there are no controllers, entities, or ingestion code. The goal is only to boot the app and run Flyway V1.

Option A (cd into module):
- `cd backend`
- Start (dev profile active): `mvn spring-boot:run`

Option B (from root using -pl):
- `mvn -pl backend spring-boot:run`

Expected:
- Flyway runs `V1__baseline.sql` successfully.
- Application starts without errors.

Config:
- Postgres via env vars: `DB_URL`, `DB_USER`, `DB_PASSWORD` (defaults provided in `application.yml`).

Note: The legacy `src/` directory at repo root is not built and can be safely deleted outside this environment.
