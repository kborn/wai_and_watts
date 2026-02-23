# Wai & Watts — Query-Backed Insights

Date: 2026-02-23
Status: Phase 15 portfolio evidence

These findings are documented as reproducible SQL over persisted tables.
For portability in-repo, each finding also notes the fixture snapshot source used to verify expected direction/value.

---

## 1) MBIE annual renewables grew from 2018 to 2024

Finding:
- Renewable generation (HYDRO + GEOTHERMAL + WIND + SOLAR) increased from **35,868.5 GWh (2018)** to **36,722.4 GWh (2024)**.
- Net change: **+853.9 GWh** (**+2.38%**).

SQL (persisted DB):
```sql
SELECT
  period_year,
  SUM(CASE WHEN fuel_type_norm IN ('HYDRO','GEOTHERMAL','WIND','SOLAR') THEN generation_gwh ELSE 0 END) AS renewable_gwh
FROM mbie_generation_annual_record
GROUP BY period_year
HAVING period_year IN (2018, 2024)
ORDER BY period_year;
```

Snapshot evidence source:
- `backend/src/test/resources/real_snapshots/expected/mbie/generation/annual/mbie_generation_annual_expected.csv`

---

## 2) MBIE quarterly solar output more than doubled (2023 Q1 -> 2025 Q3)

Finding:
- Solar generation increased from **95.5 GWh (2023 Q1)** to **211.4 GWh (2025 Q3)**.
- Approximate multiplier: **2.21x**.

SQL (persisted DB):
```sql
SELECT period_year, period_quarter, generation_gwh
FROM mbie_generation_quarterly_record
WHERE fuel_type_norm = 'SOLAR'
  AND ((period_year = 2023 AND period_quarter = 1)
    OR (period_year = 2025 AND period_quarter = 3))
ORDER BY period_year, period_quarter;
```

Snapshot evidence source:
- `backend/src/test/resources/real_snapshots/expected/mbie/generation/quarterly/mbie_generation_quarterly_expected.csv`

---

## 3) LAWA state sample is dominated by EXCELLENT classifications

Finding:
- State classification counts in the sample:
  - EXCELLENT: 25
  - GOOD: 6
  - FAIR: 2
  - POOR: 2
  - VERY_POOR: 2

SQL (persisted DB):
```sql
SELECT state_norm, COUNT(*) AS row_count
FROM lawa_state_multi_year_record
GROUP BY state_norm
ORDER BY row_count DESC;
```

Snapshot evidence source:
- `backend/src/test/resources/real_snapshots/expected/lawa/water_quality/state/multi_year/lawa_state_multi_year_expected.csv`

---

## 4) LAWA trend sample has high insufficient-data share

Finding:
- Trend classification counts in the sample:
  - INSUFFICIENT_DATA: 72
  - DEGRADING: 23
  - IMPROVING: 15

SQL (persisted DB):
```sql
SELECT trend_norm, COUNT(*) AS row_count
FROM lawa_water_quality_trend_multi_year_record
GROUP BY trend_norm
ORDER BY row_count DESC;
```

Snapshot evidence source:
- `backend/src/test/resources/real_snapshots/expected/lawa/water_quality/trend/multi_year/lawa_trend_multi_year_expected.csv`

---

## 5) Regional state composition differs between Auckland and Canterbury (sample)

Finding:
- Auckland sample rows: 21 (EXCELLENT 14, GOOD 3, FAIR 2, POOR 1, VERY_POOR 1)
- Canterbury sample rows: 16 (EXCELLENT 11, GOOD 3, POOR 1, VERY_POOR 1)

SQL (persisted DB):
```sql
SELECT region, state_norm, COUNT(*) AS row_count
FROM lawa_state_multi_year_record
WHERE region IN ('auckland', 'canterbury')
GROUP BY region, state_norm
ORDER BY region, row_count DESC;
```

Snapshot evidence source:
- `backend/src/test/resources/real_snapshots/expected/lawa/water_quality/state/multi_year/lawa_state_multi_year_expected.csv`

---

## Reproducibility Notes
- SQL targets persisted domain tables and can be run after ingestion.
- Snapshot references provide deterministic evidence in-repo when a live local DB is not running.
- Findings are descriptive only; no causal or predictive claims are made.
