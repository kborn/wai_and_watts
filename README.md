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
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"
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
