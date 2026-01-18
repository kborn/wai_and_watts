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

#### Health
- `GET /api/v1/health` — service health (and DB connectivity)

#### Dataset metadata
- `GET /api/v1/datasets` — list dataset sources + latest imported release
- `GET /api/v1/datasets/{sourceId}/releases` — list releases + status

#### Ingestion (local-only)
- `POST /api/v1/ingest/lawa/state-trend`
- `POST /api/v1/ingest/mbie/electricity`
- `POST /api/v1/ingest/mbie/renewables`

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

#### LAWA query
- `GET /api/v1/lawa/sites?q=&region=&page=&size=`
- `GET /api/v1/lawa/indicators`
- `GET /api/v1/lawa/assessments?indicator=&windowYears=&trend=&region=&page=&size=`

#### MBIE query
- `GET /api/v1/energy/generation?from=&to=&fuel=&page=&size=`
- `GET /api/v1/energy/renewable-share?from=&to=`

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
