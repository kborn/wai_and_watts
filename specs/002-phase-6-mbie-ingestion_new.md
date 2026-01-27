# Phase 6 — MBIE Electricity Ingestion Spec

We ingest MBIE electricity statistics from the Electricity data tables workbook (Sep 2025 Q3):
https://www.mbie.govt.nz/assets/Data-Files/Energy/nz-energy-quarterly-and-energy-in-nz/electricity-sept-2025-q3.xlsx

We use Sheet "6 - Fuel type (GWh)" for a long-running annual generation time series.

We do NOT use energy-overview.xlsx because it is national primary energy accounting (not grid generation).

This phase ingests dataset `mbie.generation.annual` (see design/004-mbie-schema.md for contract details).
