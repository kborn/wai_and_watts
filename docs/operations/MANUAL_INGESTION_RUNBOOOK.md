# Wai & Watts — Manual Ingestion Runbook

This runbook describes how a human operator performs manual ingestion.
It is intentionally simple and repeatable.

## Pre-requisites
- Java + Maven available locally
- Backend built: `mvn -f backend -DskipTests package`
- Postgres running and backend config set

## Step 1: Download source workbook
Use the helper scripts:
- `./scripts/download/mbie-download.sh`
- `./scripts/download/lawa-download.sh`

These save files under `./downloads/<publisher>/<YYYY-MM-DD>/`.

## Step 2: Transform XLSX to contract CSV
Use the transformer to convert the publisher workbook to the contract CSV.

```
./scripts/transform.sh <dataset_source_code> <input_xlsx_path> <output_csv_path>
```

Example:
```
./scripts/transform.sh mbie.generation.annual ./downloads/mbie/2026-02-05/electricity-sept-2025-q3.xlsx /tmp/mbie_generation_annual.csv
```

Note: Publisher sheets may include title rows, notes, or footers. The transformer targets the canonical table region; if the workbook layout has extra sections in the same sheet, trim to the table region before saving the snapshot.

## Step 3: Run manual ingestion
Use the ingestion CLI wrapper:
```
./scripts/ingest.sh <dataset_source_code> <file_path> [published_date] [release_label]
```

Example:
```
./scripts/ingest.sh mbie.generation.annual /path/to/mbie_generation_annual.csv 2025-01-01 "MBIE workbook export"
```

## Step 4: Verify success
The CLI prints:
- Release ID
- Rows persisted

Re-running on the same file should be idempotent.

## Example execution
```
./scripts/transform.sh mbie.generation.annual ./downloads/mbie/2026-02-05/electricity-sept-2025-q3.xlsx /tmp/mbie_generation_annual.csv
./scripts/ingest.sh mbie.generation.annual /tmp/mbie_generation_annual.csv 2025-09-01 "MBIE Q3 2025 workbook"
```

Expected output (shape, not exact values):
```
Starting ingestion...
Dataset: mbie.generation.annual
File: /tmp/mbie_generation_annual.csv
Published Date: 2025-09-01
Release Label: MBIE Q3 2025 workbook
SUCCESS
Release ID: <uuid>
Rows persisted: <count>
```

## Troubleshooting
- If parsing fails, confirm the transformer output CSV contains only the canonical table (no titles, notes, or footers).
- If required columns are missing, verify the source sheet and transformer mapping.
- If the file is truncated or header-only, re-export from the workbook and ensure at least one data row is included.
