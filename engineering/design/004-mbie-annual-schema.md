# MBIE — Electricity Generation (Phase 6) — Schema & Fixture Contract

## Dataset Identity

Canonical Dataset Source Code: `mbie.generation.annual`

This identifier is the stable key used in:
- dataset_source.code (database)
- ingestion orchestration
- API routing and documentation
- fact-pack and LLM grounding layers


## Source
Workbook: electricity-sept-2025-q3.xlsx  
URL:https://www.mbie.govt.nz/assets/Data-Files/Energy/nz-energy-quarterly-and-energy-in-nz/electricity-sept-2025-q3.xlsx  
Sheet: "6 - Fuel type (GWh)"  
Unit: GWh

## Table: mbie_generation_record

| Column | Type | Notes |
|--------|------|-------|
| id | UUID/BIGINT | PK |
| dataset_release_id | FK | lineage |
| period_year | INT | calendar year |
| fuel_type_raw | TEXT | raw label |
| fuel_type_norm | TEXT | normalized token |
| generation_gwh | DECIMAL | net generation |

Unique: (dataset_release_id, period_year, fuel_type_norm)

## Fuel Normalization
Hydro→HYDRO, Geothermal→GEOTHERMAL, Wind→WIND, Solar→SOLAR, Natural gas→GAS, Coal→COAL, else OTHER.

## Fixture Contract
CSV header:
period_year,fuel_type_raw,fuel_type_norm,generation_gwh

Example:
2022,Hydro,HYDRO,26071
2022,Wind,WIND,2835
