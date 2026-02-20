# Wai & Watts

Wai & Watts is a contract-first data ingestion and normalization platform focused on real-world New Zealand environmental and energy datasets.

Phase 10 provides a **human-operated** operator workflow:

download → transform → ingest (CLI) → start backend → validate APIs

## Start here

**Canonical operator guide:**

- `docs/operators/OPERATOR_INGESTION_GUIDE.md`

**Validation exercise / portfolio artifact:**

- `docs/validation/PHASE10_OPERATOR_TEST_DRIVE.md`

## Quick Start

1. **Prerequisites**
   ```bash
   # Verify Java 21
   java -version
   
   # Verify Maven
   mvn -v
   
   # Install and start Postgres locally
   ```

2. **Environment Setup**
   ```bash
   # Set database credentials
   export DB_URL="jdbc:postgresql://localhost:5432/waiwatts"
   export DB_USER="waiwatts"
   export DB_PASSWORD="waiwatts"
   ```

   Optional LLM configuration (if unset, the app uses deterministic stub responses):
   ```bash
   # OpenAI provider + model + key
   export LLM_PROVIDER="OPENAI"
   export LLM_MODEL="gpt-4o-mini"
   export LLM_API_KEY="your_api_key"
   export LLM_BASE_URL="https://api.openai.com/v1"
   ```

3. **Build**
   ```bash
   # Build executable Spring Boot JAR
   mvn -f backend clean package spring-boot:repackage -DskipTests
   
   # Make scripts executable
   chmod +x scripts/*.sh scripts/download/*.sh
   ```

## Phase 10 execution model (strict)

- Ingestion is **CLI-driven only** via `./scripts/ingest.sh` (no HTTP ingestion in the operator path).
- The backend server is **NOT required** to perform ingestion.
- The backend server **IS required** for post-ingestion API validation (curl examples in the test drive).
- Internal ingestion endpoints (dev/test) are not operator workflows and should not be referenced in operator docs.

## API versioning policy

- Public endpoints are versioned under `/api/v1/...`.
- `v1` is the current stable contract prefix; route changes that break compatibility require a new major prefix (for example, `/api/v2/...`).
- Internal/dev-only routes, when present, must be explicitly namespaced under `/api/v1/internal/...` and are not public API contracts.

## `dataset_release` semantics

- `dataset_release` is the lineage boundary for one ingested publisher artifact version.
- Domain rows always link to exactly one `dataset_release_id`.
- Read endpoints are release-transparent row APIs: they return stored rows and include `releaseId` on each row, rather than collapsing multiple releases into one synthesized aggregate.
- Ask (`/api/v1/explanations/ask`) is release-pinned: Fact Pack builders deterministically select one canonical `dataset_release` per request before building facts/citations.

## Supported datasets (Phase 10)

- `mbie.generation.annual`
- `mbie.generation.quarterly`
- `lawa.water_quality.state.multi_year`
- `lawa.water_quality.trend.multi_year`

## Development

### Running the Backend

**Normal mode:**
```bash
cd backend
mvn spring-boot:run
```

**Debug mode:**
```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005"
```

### Debugging with IntelliJ

**Option A: Create Remote Debug Configuration**
1. **Run → Edit Configurations → Add → Remote JVM Debug**
2. **Configure:**
   - Name: `Wai & Watts Debug`
   - Host: `localhost`
   - Port: `5005`
   - Transport: `Socket`
   - Debugger mode: `Attach`
3. **Click "Debug"** to connect

**Option B: Attach to Running Process**
1. **Run → Attach to Process**
2. **Select the Wai & Watts Java process**
3. **Click "Attach"**

**Debug startup modes:**
- `suspend=n` - Backend starts immediately, debug port available
- `suspend=y` - Backend waits for debugger connection before starting

### Frontend Development

```bash
cd frontend
npm install
npm run dev
```

Access frontend at http://localhost:5173 (or next available port).

### Full Stack Development

1. **Start backend:**
   ```bash
   cd backend && mvn spring-boot:run
   ```

2. **Start frontend:**
   ```bash
   cd frontend && npm run dev
   ```

3. **Access application:**
   - Frontend: http://localhost:5173
   - Backend API: http://localhost:8080
   - API docs: http://localhost:8080/swagger-ui.html

---

## Docker (Recommended for Quick Start)

### Prerequisites
- Docker
- Docker Compose (included with Docker Desktop)

### Quick Start

```bash
# Optional: enable LLM provider for /api/v1/explanations
export LLM_MODEL="gpt-4o-mini"
export LLM_API_KEY="your_api_key"

# Start all services (Postgres + Backend + Frontend)
docker compose up -d --build

# View logs
docker compose logs -f

# Stop all services
docker compose down
```

### Services
| Service    | URL                          | Description              |
|------------|------------------------------|------------------------|
| Frontend   | http://localhost:5173       | React UI               |
| Backend    | http://localhost:8080       | Spring Boot API        |
| Postgres   | localhost:5432              | Database (waiwatts)   |

### Data Population

Data files are bundled in the container at `/app/downloads/`.
To run ingestion per dataset (transform + ingest), use the pipeline entrypoint:

```bash
# MBIE annual generation
docker compose run --rm ingest \
  mbie.generation.annual --bundle-date 2026-02-06 \
  --published-date 2025-11-01 --release-label "MBIE Q3 2025"

# MBIE quarterly generation
docker compose run --rm ingest \
  mbie.generation.quarterly --bundle-date 2026-02-06 \
  --published-date 2025-11-01 --release-label "MBIE Q3 2025"

# LAWA state (multi-year)
docker compose run --rm ingest \
  lawa.water_quality.state.multi_year --bundle-date 2026-02-06 \
  --published-date 2025-10-15 --release-label "LAWA Oct 2025"

# LAWA trend (multi-year)
docker compose run --rm ingest \
  lawa.water_quality.trend.multi_year --bundle-date 2026-02-06 \
  --published-date 2025-10-15 --release-label "LAWA Oct 2025"
```

The pipeline will:
1. Transform XLSX files to CSV
2. Ingest into the database
3. Handle idempotency (skip if already ingested)

Notes:
- `--bundle-date YYYY-MM-DD` derives the workbook path from `downloads/<provider>/<date>/...`.
- `--input /path/to/workbook.xlsx` overrides the derived path.
- `--published-date` and `--release-label` are optional flags.
- If `downloads/manifest/<bundle-date>.json` exists, `--published-date` and `--release-label` can be omitted.

Example (manifest-driven):

```bash
docker compose run --rm ingest-all --bundle-date 2026-02-06
```

For full operator workflow, see:
- `docs/operators/OPERATOR_INGESTION_GUIDE.md`
- `docs/validation/PHASE10_OPERATOR_TEST_DRIVE.md`

### With Local Development

For development with hot reload, use local services instead:

```bash
# Terminal 1: Start backend
cd backend && mvn spring-boot:run

# Terminal 2: Start frontend
cd frontend && npm run dev

# Terminal 3: Start Postgres (or use local installation)
docker run -p 5432:5432 -e POSTGRES_DB=waiwatts -e POSTGRES_USER=waiwatts -e POSTGRES_PASSWORD=waiwatts postgres:16-alpine
```

### Manual Docker Build (Advanced)

```bash
# Build backend image
docker build -t waiwatts-backend:latest -f backend/Dockerfile .

# Build frontend image
docker build -t waiwatts-frontend:latest -f frontend/Dockerfile .

# Run with docker-compose using local builds
docker compose up -d --build
```
