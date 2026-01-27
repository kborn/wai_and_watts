# MBIE Quarterly Generation --- Schema & Fixture Contract (Phase 7)

Dataset Source Code: `mbie.generation.quarterly`

## Source

Workbook: https://www.mbie.govt.nz/assets/Data-Files/Energy/nz-energy-quarterly-and-energy-in-nz/electricity-sept-2025-q3.xlsx

Sheet: `1 - Fuel type (GWh)`

Units: GWh (net electricity generation delivered to grid)

Temporal Model: (year, quarter)

## Semantics Guarantees

- generation_gwh represents net generation delivered to the grid
- Values are reported by MBIE as finalized quarterly aggregates
- No conversion from PJ or MWh occurs in Phase 7


## Domain Table

Table Name: `mbie_generation_quarterly_record`

### Columns

  ------------------------------------------------------------------------
  Column                Type            Description
  --------------------- --------------- ----------------------------------
  id                    bigint PK       Surrogate key

  year                  int             Calendar year

  quarter               int             1--4

  fuel_type_raw         text            Raw MBIE fuel label

  fuel_type_norm        text/enum       Normalized fuel category (HYDRO,
                                        WIND, SOLAR, GEOTHERMAL, GAS,
                                        COAL, OTHER)

  generation_gwh        decimal         Generation in GWh

  dataset_release_id    bigint FK       Lineage reference
  ------------------------------------------------------------------------

### Constraints

-   UNIQUE(dataset_release_id, year, quarter, fuel_type_norm)
-   NOT NULL on all domain columns

## Fixture Contract

### CSV Header

year,quarter,fuel_type_raw,fuel_type_norm,generation_gwh

### Example Row

2024,3,Hydro,HYDRO,10532.4

## Normalization Rules

-   Preserve fuel_type_raw exactly as published
-   fuel_type_norm is uppercase canonical category
-   Unknown categories map to OTHER

## Notes

-   Annual and quarterly datasets remain separate tables
-   Unification deferred to later phases via views or derived tables
