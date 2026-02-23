# Example Live Ingestion Execution

This is a concrete example of a manual ingestion run.

## MBIE Annual
```
./scripts/transform.sh mbie.generation.annual ./downloads/mbie/2026-02-05/electricity-generation-quarterly-and-annual-data-2025-quarter-3.xlsx /tmp/mbie_generation_annual.csv
./scripts/ingest.sh mbie.generation.annual /tmp/mbie_generation_annual.csv 2025-09-01 "MBIE Q3 2025 workbook"
```

## MBIE Quarterly
```
./scripts/transform.sh mbie.generation.quarterly ./downloads/mbie/2026-02-05/electricity-generation-quarterly-and-annual-data-2025-quarter-3.xlsx /tmp/mbie_generation_quarterly.csv
./scripts/ingest.sh mbie.generation.quarterly /tmp/mbie_generation_quarterly.csv 2025-09-01 "MBIE Q3 2025 workbook"
```

## LAWA State (Multi-Year)
```
./scripts/transform.sh lawa.water_quality.state.multi_year ./downloads/lawa/2026-02-05/NZ-State-and-Trend-Water-Quality-Data-2025.xlsx /tmp/lawa_state_multi_year.csv
./scripts/ingest.sh lawa.water_quality.state.multi_year /tmp/lawa_state_multi_year.csv 2025-10-30 "LAWA State + Trend 2025"
```

## LAWA Trend (Multi-Year)
```
./scripts/transform.sh lawa.water_quality.trend.multi_year ./downloads/lawa/2026-02-05/NZ-State-and-Trend-Water-Quality-Data-2025.xlsx /tmp/lawa_trend_multi_year.csv
./scripts/ingest.sh lawa.water_quality.trend.multi_year /tmp/lawa_trend_multi_year.csv 2025-10-30 "LAWA State + Trend 2025"
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
