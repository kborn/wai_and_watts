# Wai & Watts — 002 Contracts

**Status:** Interfaces doc  
**Last updated:** 2026-01-17  
**Audience:** Builder GPT

This document defines the external and internal “interfaces”:
- API contracts (requests/responses, error model)
- Data contracts (assumptions about LAWA/MBIE artifacts)

---

## 1. API Contract

### 1.1 Principles
- Versioned under `/api/v1`
- JSON only
- Pagination for list endpoints
- Consistent error envelope
- “Insights” endpoints must include `factPack`

### 1.2 Standard error envelope
```json
{
  "code": "STRING_CODE",
  "message": "Human-readable summary",
  "details": "Optional diagnostic detail",
  "traceId": "uuid"
}
```

### 1.3 Pagination response envelope
```json
{
  "items": [],
  "page": 0,
  "size": 25,
  "totalItems": 0,
  "totalPages": 0
}
```

### 1.4 Endpoints (MVP)

#### Health & System
- `GET /api/v1/health` — global app health check
- `GET /api/v1/insights` — returns Insights.md content as markdown

#### Dataset metadata
- `GET /api/v1/datasets/sources` — list dataset sources + latest imported release
- `GET /api/v1/datasets/sources/{id}/releases` — list releases + status

#### Ingestion (dev-only, profile-guarded)
- `POST /api/v1/internal/ingest` — manual data ingestion

Request body:
```json
{ "sourceUrl": "https://...", "publishedDate": "YYYY-MM-DD" }
```

Response body:
```json
{
  "datasetReleaseId": "uuid",
  "status": "IMPORTED",
  "rowsRead": 0,
  "rowsInserted": 0,
  "rowsUpdated": 0,
  "warnings": []
}
```

#### MBIE electricity generation data
- `GET /api/v1/mbie/generation/annual` — annual generation data (supports fuelType filter)
- `GET /api/v1/mbie/generation/quarterly` — quarterly generation data (supports fuelType filter)

Query parameters (both endpoints):
- `fromYear` (optional) — filter from year
- `toYear` (optional) — filter to year  
- `source` (optional) — filter by fuel source
- `fuelType` (optional) — filter by fuel type

#### LAWA water quality data
- `GET /api/v1/lawa/water-quality/state/multiyear` — water quality state assessments
- `GET /api/v1/lawa/water-quality/trend/multiyear` — water quality trend analyses

#### Insights (fact-pack-first)
- `GET /api/v1/insights/overview?from=&to=&region=&indicator=&windowYears=`

Insights response MUST include:
- `factPack.sources[]`: dataset name, publisher, sourceUrl, publishedDate, importedAt
- `factPack.caveats[]`: plain-language caveats about what the data is (and isn’t)

---

## 2. Data Contracts

### 2.1 Global ingestion rules
- Column order is not trusted; prefer header-based mapping.
- Unknown columns are ignored but logged.
- Missing optional fields do not fail ingestion.
- Missing required fields: skip row + log warning (unless systemic).

### 2.2 LAWA — state & trend results
**Nature of data**
- Derived assessments per site/indicator/window — not raw sensor feeds.

**Required fields (conceptual)**
- Site identifier
- Site name
- Indicator code/name
- Trend window (years)
- Trend direction

**Optional fields**
- Trend slope, significance
- State class/band, state value
- Region/council
- Coordinates (lat/lon)
- Period start/end

**Enum mapping**
Trend direction must map to:
- IMPROVING | WORSENING | NO_TREND | INSUFFICIENT_DATA
Unknown direction values should fail ingestion fast with a clear message (structural change).

### 2.3 MBIE — electricity generation spreadsheet
**Nature of data**
- Quarterly generation values by fuel.

**Required fields (conceptual)**
- Quarter/period
- Fuel type/name
- Generation value (GWh)

**Normalization**
- Fuel names are normalized via a mapping into `energy_fuel.code`.
- The mapping must be explicit and reviewed (code + display_name + is_renewable).

### 2.4 MBIE — renewables statistics spreadsheet
**Nature of data**
- Annual metrics; table shapes may vary across releases.

**Required fields (conceptual)**
- Year
- Resource type
- Metric name
- Numeric value

**Storage approach**
- Store as flexible metric rows (`mbie_renewables_stat`) rather than strongly typed columns.

---

## 3. Contract change handling
- Additive columns: tolerate automatically.
- Column rename / table restructure:
  - ingestion must fail fast
  - error message must name the missing/changed fields
  - update this doc + ingestion logic together.
