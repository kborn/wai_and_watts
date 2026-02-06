# Operator Ingestion Guide (Phase 10)

## Purpose

This is the **single source of truth** for running Phase 10 ingestion locally.

If this document conflicts with any other document, this document is correct.

## Supported dataset codes

These are the dataset source codes accepted by both `transform.sh` and `ingest.sh`:

- `mbie.generation.annual`
- `mbie.generation.quarterly`
- `lawa.water_quality.state.multi_year`
- `lawa.water_quality.trend.multi_year`

Notes:
- MBIE annual + quarterly both come from the same MBIE workbook.
- LAWA state + trend both come from the same LAWA workbook.

## Am I ready? (quick checklist)

- [ ] `java -version` shows JDK 21
- [ ] `mvn -v` works
- [ ] Postgres is running locally and reachable
- [ ] Backend builds: `mvn -f backend clean package spring-boot:repackage -DskipTests`
- [ ] Scripts are executable: `chmod +x ./scripts/*.sh`
- [ ] You can download a publisher workbook into `./downloads/<publisher>/<YYYY-MM-DD>/`

## Database setup (local Postgres)

This project expects Postgres running locally and backend configured via environment variables:

- `DB_URL`
- `DB_USER` 
- `DB_PASSWORD`

Example (adjust to your machine):

```bash
export DB_URL="jdbc:postgresql://localhost:5432/waiwatts"
export DB_USER="waiwatts"
export DB_PASSWORD="waiwatts"
```

### Quick Postgres setup

**Option 1: Using Homebrew (macOS)**
```bash
# Install and start Postgres
brew install postgresql
brew services start postgresql

# Create database and user
createdb waiwatts
createuser waiwatts
psql -d postgres -c "ALTER USER waiwatts WITH PASSWORD 'waiwatts';"
psql -d postgres -c "GRANT ALL PRIVILEGES ON DATABASE waiwatts TO waiwatts;"
```

**Option 2: Using Docker (all platforms)**
```bash
# Start Postgres container
docker run --name waiwatts-postgres \
  -e POSTGRES_DB=waiwatts \
  -e POSTGRES_USER=waiwatts \
  -e POSTGRES_PASSWORD=waiwatts \
  -p 5432:5432 \
  -d postgres:15

# Use this connection string instead:
export DB_URL="jdbc:postgresql://localhost:5432/waiwatts"
export DB_USER="waiwatts"
export DB_PASSWORD="waiwatts"
```

### Important: Database Migration

Before running ingestion, you must run the Spring Boot application **once** to execute database migrations:

```bash
# This runs all Flyway migrations to create tables and dataset source records
mvn -f backend spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=default"

# Or after building:
java -jar backend/target/backend-0.0.1-SNAPSHOT.jar

# Stop the application after migrations complete (Ctrl+C)
```

The migrations create:
- Database tables and indexes
- Required dataset source records (`mbie.generation.annual`, `mbie.generation.quarterly`, etc.)

Without this step, ingestion will fail with "Unknown dataset source code" errors.

If you need a quick local DB:

```bash
createdb waiwatts
createuser waiwatts
psql -d postgres -c "ALTER USER waiwatts WITH PASSWORD 'waiwatts';"
psql -d postgres -c "GRANT ALL PRIVILEGES ON DATABASE waiwatts TO waiwatts;"
```

## Step 1 — Download publisher workbook

Use the helper scripts (these save under `./downloads/<publisher>/<YYYY-MM-DD>/`):

- `./scripts/download/mbie-download.sh`
- `./scripts/download/lawa-download.sh`

Run them, then confirm you have an XLSX at a path like:

- `./downloads/mbie/<YYYY-MM-DD>/electricity-generation-quarterly-and-annual-data-2025-quarter-3.xlsx`
- `./downloads/lawa/<YYYY-MM-DD>/NZ-State-and-Trend-Water-Quality-Data-2025.xlsx`

## Step 2 — Transform XLSX → contract CSV

The transform script automatically saves to `./transforms/<dataset_code>/<YYYY-MM-DD>/` with auto-generated filenames.

Usage:

```bash
./scripts/transform.sh <dataset_source_code> <input_xlsx_path> [OPTIONS]
```

Examples:

```bash
# Auto-generated directory and filename
./scripts/transform.sh mbie.generation.annual ./downloads/mbie/2026-02-05/electricity-sept-2025-q3.xlsx

# Custom output directory
./scripts/transform.sh mbie.generation.quarterly ./downloads/mbie/2026-02-05/electricity-sept-2025-q3.xlsx --output-dir /tmp

# Custom filename
./scripts/transform.sh lawa.water_quality.state.multi_year ./downloads/lawa/2026-02-05/lawa-workbook.xlsx --output-file lawa_state.csv

# Both custom directory and filename
./scripts/transform.sh lawa.water_quality.trend.multi_year ./downloads/lawa/2026-02-05/lawa-workbook.xlsx --output-dir /tmp --output-file trend.csv
```

Output locations:
- **Default**: `./transforms/mbie.generation.annual/2026-02-05/mbie_generation_annual.csv`
- **Default**: `./transforms/mbie.generation.quarterly/2026-02-05/mbie_generation_quarterly.csv`
- **Default**: `./transforms/lawa.water_quality.state.multi_year/2026-02-05/lawa_state_multi_year.csv`
- **Default**: `./transforms/lawa.water_quality.trend.multi_year/2026-02-05/lawa_trend_multi_year.csv`

## Step 3 — Ingest contract CSV (CLI)

Usage:

```bash
./scripts/ingest.sh <dataset_source_code> <file_path> [published_date] [release_label]
```

Concrete examples (using default transform locations):

```bash
./scripts/ingest.sh mbie.generation.annual ./transforms/mbie.generation.annual/2026-02-05/mbie_generation_annual.csv 2025-09-01 "MBIE Q3 2025 workbook"
./scripts/ingest.sh mbie.generation.quarterly ./transforms/mbie.generation.quarterly/2026-02-05/mbie_generation_quarterly.csv 2025-09-01 "MBIE Q3 2025 workbook"
./scripts/ingest.sh lawa.water_quality.state.multi_year ./transforms/lawa.water_quality.state.multi_year/2026-02-05/lawa_state_multi_year.csv 2025-10-30 "LAWA State + Trend 2025"
./scripts/ingest.sh lawa.water_quality.trend.multi_year ./transforms/lawa.water_quality.trend.multi_year/2026-02-05/lawa_trend_multi_year.csv 2025-10-30 "LAWA State + Trend 2025"
```

Important:
- The backend server must **not** be required to perform ingestion.
- The operator workflow does **not** use internal ingestion HTTP endpoints.

## Step 4 — Validate via API (server required for validation)

Start the backend server:

```bash
mvn -pl backend spring-boot:run
```

Then run API validation calls:

```bash
curl "http://localhost:8080/api/v1/mbie/generation"
curl "http://localhost:8080/api/v1/mbie/generation/quarterly"
curl "http://localhost:8080/api/v1/lawa/state/multiyear"
curl "http://localhost:8080/api/v1/lawa/trend/multiyear"
```

## Troubleshooting (operator-first)

### Transform Issues
- If transform fails: confirm the workbook is the expected publisher file and the transformer mapping is current.
- Check that the executable Spring Boot JAR exists (69MB+, not 144KB).

### Ingestion Issues  
- If ingestion fails with "Unknown dataset source code": 
  1. Confirm database migrations have run: `mvn -f backend spring-boot:run`
  2. Stop application after migrations complete (Ctrl+C)
  3. Verify migrations created dataset source records
- If ingestion fails with CSV parsing: confirm the contract CSV contains only canonical table (no titles/notes/footers).

### API Validation Issues
- If API validation fails: confirm the backend server is running and Postgres credentials are correct.
- Check application logs for database connection errors.

### Common Database Issues
- **Connection refused**: Postgres not running or wrong port
- **Authentication failed**: Wrong DB_USER or DB_PASSWORD
- **Database does not exist**: Run `createdb waiwatts` first
- **No migrations applied**: Run Spring Boot app once to execute Flyway migrations
- **Flyway validation errors**: Database may be in inconsistent state, requiring database reset
