# Example Live Ingestion Execution

This is a concrete example of a manual ingestion run.

## MBIE Annual
```
./scripts/transform.sh mbie.generation.annual ./downloads/mbie/2026-02-05/electricity-sept-2025-q3.xlsx /tmp/mbie_generation_annual.csv
./scripts/ingest.sh mbie.generation.annual /tmp/mbie_generation_annual.csv 2025-09-01 "MBIE Q3 2025 workbook"
```

Expected output (shape, not exact values):
```
Starting ingestion...
Dataset: mbie.generation.annual
File: ./downloads/mbie/2026-02-05/mbie_generation_annual.csv
Published Date: 2025-09-01
Release Label: MBIE Q3 2025 workbook
SUCCESS
Release ID: <uuid>
Rows persisted: <count>
```
