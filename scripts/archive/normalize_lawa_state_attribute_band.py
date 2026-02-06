#!/usr/bin/env python3
"""
------------------------------------------------------------------------------------------------------------------------------
*Note!  Historical doc. Used to create early fixtures but now not the current ingestion path. Replaced by scripts/transform.sh
------------------------------------------------------------------------------------------------------------------------------
"""

"""Normalize LAWA 'State Attribute Band' into a deterministic Phase 8 fixture CSV.

Dataset Source Code: lawa.water_quality.state.multi_year
Sheet: State Attribute Band

Period contract (Phase 8):
- period_type = HYDRO_5YR_ROLLING
- period_end_year = hYear
- period_start_year = hYear - 5

This script is intentionally fixture-oriented: it extracts a small, representative slice
and writes the exact CSV header expected by the Phase 8 schema.

Usage:
  python scripts/normalize_lawa_state_attribute_band.py \
      --input "/path/to/lawa_state_workbook.xlsx" \
      --output "fixtures/lawa/state_multi_year_fixture.csv" \
      --regions auckland canterbury \
      --sites-per-region 3
"""

from __future__ import annotations

import argparse
from pathlib import Path
import pandas as pd

PERIOD_TYPE = "HYDRO_5YR_ROLLING"

BAND_TO_STATE = {
    "A": "EXCELLENT",
    "B": "GOOD",
    "C": "FAIR",
    "D": "POOR",
    "E": "VERY_POOR",
}

# Keep normalization deliberately small + stable for Phase 8.
INDICATOR_NORM_MAP = {
    "E.coli": "ECOLI",
    "Clarity / Suspended fine sediment": "CLARITY",
    "Dissolved reactive phosphorus": "DRP",
    "NO3N": "NO3N",
    "TON": "TON",
    "Ammonical nitrogen / Ammonia (toxicity)": "AMMONIA_TOXICITY",
    "Nitrate nitrogen / Nitrate (toxicity)": "NITRATE_TOXICITY",
}

SOURCE_COLUMNS = {
    "Region": "region",
    "LawaSiteID": "lawa_site_id",
    "SiteID": "site_name",
    "hYear": "hYear",
    "Latitude": "latitude",
    "Longitude": "longitude",
    "Indicator / Attribute": "indicator_raw",
    "UnitsOfMeasure": "units",
    "Attribute Band": "attribute_band",
    "Median": "median",
    "95th Percentile": "p95",
    "RecHealth_% exceedances over 260_numeric attribute statistic": "rec_health_exceed_260_pct",
    "RecHealth_% exceedances over 540_numeric attribute statistic": "rec_health_exceed_540_pct",
}

FIXTURE_COLUMNS = [
    "lawa_site_id",
    "site_name",
    "region",
    "latitude",
    "longitude",
    "indicator_raw",
    "indicator_norm",
    "units",
    "attribute_band",
    "state_norm",
    "median",
    "p95",
    "rec_health_exceed_260_pct",
    "rec_health_exceed_540_pct",
    "period_type",
    "period_start_year",
    "period_end_year",
]


def parse_args() -> argparse.Namespace:
    p = argparse.ArgumentParser()
    p.add_argument("--input", required=True, help="Path to LAWA workbook (.xlsx)")
    p.add_argument("--output", required=True, help="Path to output fixture CSV")
    p.add_argument("--sheet", default="State Attribute Band", help="Excel sheet name")
    p.add_argument("--regions", nargs="+", default=["auckland", "canterbury"], help="Regions to include (case-insensitive)")
    p.add_argument("--sites-per-region", type=int, default=3, help="Number of sites to include per region")
    return p.parse_args()


def normalize_indicator(raw: str) -> str:
    raw = (raw or "").strip()
    return INDICATOR_NORM_MAP.get(raw, "OTHER")


def normalize_state_band(band: str) -> str:
    band = (band or "").strip().upper()
    if band not in BAND_TO_STATE:
        raise ValueError(f"Unexpected Attribute Band: {band!r}")
    return BAND_TO_STATE[band]


def pick_sites(df: pd.DataFrame, region: str, n: int) -> list[str]:
    sub = df[df["region"].str.lower() == region.lower()].copy()
    sub = sub.sort_values(["site_name", "lawa_site_id"])
    return sub["lawa_site_id"].dropna().unique().tolist()[:n]


def main() -> None:
    args = parse_args()

    in_path = Path(args.input)
    out_path = Path(args.output)
    out_path.parent.mkdir(parents=True, exist_ok=True)

    df_raw = pd.read_excel(in_path, sheet_name=args.sheet)

    missing = [c for c in SOURCE_COLUMNS if c not in df_raw.columns]
    if missing:
        raise RuntimeError(f"Missing expected columns in sheet {args.sheet!r}: {missing}")

    df = df_raw[list(SOURCE_COLUMNS.keys())].rename(columns=SOURCE_COLUMNS)

    # Cleanup without converting NaNs into literal 'nan' strings
    for col in ["region", "lawa_site_id", "site_name", "indicator_raw", "units", "attribute_band"]:
        df[col] = df[col].where(df[col].isna(), df[col].astype(str).str.strip())

    df["hYear"] = df["hYear"].astype(int)

    # Keep only known indicators for fixture stability
    df = df[df["indicator_raw"].isin(INDICATOR_NORM_MAP.keys())].copy()

    # Drop rows that have no band (cannot derive state_norm)
    df = df[df["attribute_band"].notna()].copy()

    # Choose a deterministic site slice per region
    wanted_regions = [r.lower() for r in args.regions]
    df = df[df["region"].astype(str).str.lower().isin(wanted_regions)].copy()

    chosen_sites: list[str] = []
    for r in wanted_regions:
        chosen_sites.extend(pick_sites(df, r, args.sites_per_region))
    df = df[df["lawa_site_id"].isin(chosen_sites)].copy()

    # Derive normalized fields
    df["indicator_norm"] = df["indicator_raw"].map(normalize_indicator)
    df["state_norm"] = df["attribute_band"].map(normalize_state_band)

    # Period contract
    df["period_type"] = PERIOD_TYPE
    df["period_end_year"] = df["hYear"].astype(int)
    df["period_start_year"] = df["period_end_year"] - 5

    df_out = df[FIXTURE_COLUMNS].copy()
    df_out = df_out.sort_values(["region", "lawa_site_id", "indicator_norm", "period_end_year"])
    df_out.to_csv(out_path, index=False)

    print(f"Wrote {len(df_out)} rows to {out_path}")
    print(f"Regions included: {wanted_regions}")
    print(f"Sites chosen: {chosen_sites}")


if __name__ == "__main__":
    main()
