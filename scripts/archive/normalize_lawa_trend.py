#!/usr/bin/env python3
"""
------------------------------------------------------------------------------------------------------------------------------
*Note!  Historical doc. Used to create early fixtures but now not the current ingestion path. Replaced by scripts/transform.sh
------------------------------------------------------------------------------------------------------------------------------
"""

"""Normalize LAWA 'Trend' sheet into a deterministic Phase 9 fixture CSV.

Dataset Source Code: lawa.water_quality.trend.multi_year
Workbook: lawa-river-water-quality-state-and-trend-results_30oct2025.xlsx
Sheet: Trend

Period contract (Phase 9):
- as_of_year = MAX(hYear) from State Attribute Band in the same workbook
- period_type = HYDRO_NYR_WINDOW
- period_end_year = as_of_year
- period_start_year = as_of_year - trend_period_years

Usage:
  python scripts/normalize_lawa_trend.py \
      --input "/path/to/lawa_state_and_trend.xlsx" \
      --output "fixtures/lawa/trend_multi_year_fixture.csv" \
      --regions auckland canterbury \
      --sites-per-region 3
"""

from __future__ import annotations

import argparse
from pathlib import Path
import pandas as pd

PERIOD_TYPE = "HYDRO_NYR_WINDOW"

INDICATOR_NORM_MAP = {
    "E.coli": "ECOLI",
    "Clarity": "CLARITY",
    "Dissolved reactive phosphorus": "DRP",
    "Nitrate nitrogen": "NITRATE_N",
    "Total nitrogen": "TOTAL_N",
    "Ammoniacal nitrogen": "AMMONIACAL_N",
}

SOURCE_COLUMNS = {
    "Region": "region",
    "LawaSiteID": "lawa_site_id",
    "SiteID": "site_name",
    "Latitude": "latitude",
    "Longitude": "longitude",
    "Indicator": "indicator_raw",
    "TrendPeriod (year)": "trend_period_years",
    "TrendDataFrequency": "trend_data_frequency",
    "TrendScore": "trend_score",
    "TrendDescription": "trend_raw",
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
    "trend_raw",
    "trend_norm",
    "trend_score",
    "trend_period_years",
    "trend_data_frequency",
    "period_type",
    "period_start_year",
    "period_end_year",
]


def parse_args() -> argparse.Namespace:
    p = argparse.ArgumentParser()
    p.add_argument("--input", required=True, help="Path to LAWA workbook (.xlsx)")
    p.add_argument("--output", required=True, help="Path to output fixture CSV")
    p.add_argument("--trend-sheet", default="Trend", help="Excel sheet name for trends")
    p.add_argument("--state-sheet", default="State Attribute Band", help="Excel sheet name for state (as_of_year derivation)")
    p.add_argument("--regions", nargs="+", default=["auckland", "canterbury"], help="Regions to include (case-insensitive)")
    p.add_argument("--sites-per-region", type=int, default=3, help="Number of sites to include per region")
    return p.parse_args()


def normalize_indicator(raw: str) -> str:
    raw = (raw or "").strip()
    return INDICATOR_NORM_MAP.get(raw, "OTHER")


def normalize_trend(desc: str) -> str:
    if desc is None or (isinstance(desc, float) and pd.isna(desc)):
        return "INSUFFICIENT_DATA"
    d = str(desc).strip().lower()
    if "improving" in d:
        return "IMPROVING"
    if "degrading" in d:
        return "DEGRADING"
    if "no change" in d or "no trend" in d:
        return "NO_CHANGE"
    # includes: 'Not determined', 'Indeterminate', and other non-directional labels
    return "INSUFFICIENT_DATA"


def pick_sites(df: pd.DataFrame, region: str, n: int) -> list[str]:
    sub = df[df["region"].str.lower() == region.lower()].copy()
    sub = sub.sort_values(["site_name", "lawa_site_id"])
    return sub["lawa_site_id"].dropna().unique().tolist()[:n]


def main() -> None:
    args = parse_args()
    in_path = Path(args.input)
    out_path = Path(args.output)
    out_path.parent.mkdir(parents=True, exist_ok=True)

    # Derive as_of_year from the State sheet
    state_df = pd.read_excel(in_path, sheet_name=args.state_sheet)
    if "hYear" not in state_df.columns:
        raise RuntimeError(f"Expected 'hYear' column in state sheet {args.state_sheet!r}")
    as_of_year = int(state_df["hYear"].dropna().max())

    raw = pd.read_excel(in_path, sheet_name=args.trend_sheet)

    missing = [c for c in SOURCE_COLUMNS if c not in raw.columns]
    if missing:
        raise RuntimeError(f"Missing expected columns in trend sheet {args.trend_sheet!r}: {missing}")

    df = raw[list(SOURCE_COLUMNS.keys())].rename(columns=SOURCE_COLUMNS)

    # Cleanup without converting NaNs to literal strings
    for col in ["region", "lawa_site_id", "site_name", "indicator_raw", "trend_raw"]:
        df[col] = df[col].where(df[col].isna(), df[col].astype(str).str.strip())

    df["region"] = df["region"].astype(str).str.strip().str.lower()
    df["trend_period_years"] = df["trend_period_years"].astype(int)
    df["trend_score"] = df["trend_score"].astype(int)

    # Filter regions + deterministic site slice per region
    wanted_regions = [r.lower() for r in args.regions]
    df = df[df["region"].isin(wanted_regions)].copy()

    chosen_sites: list[str] = []
    for r in wanted_regions:
        chosen_sites.extend(pick_sites(df, r, args.sites_per_region))
    df = df[df["lawa_site_id"].isin(chosen_sites)].copy()

    # Keep only known indicators for fixture stability
    df = df[df["indicator_raw"].isin(INDICATOR_NORM_MAP.keys())].copy()

    # Derive normalized fields + period fields
    df["indicator_norm"] = df["indicator_raw"].map(normalize_indicator)
    df["units"] = None  # not published in trend export
    df["trend_norm"] = df["trend_raw"].map(normalize_trend)

    df["period_type"] = PERIOD_TYPE
    df["period_end_year"] = as_of_year
    df["period_start_year"] = df["period_end_year"] - df["trend_period_years"]

    out = df[FIXTURE_COLUMNS].copy()
    out = out.sort_values(["region", "lawa_site_id", "indicator_norm", "trend_period_years"])
    out.to_csv(out_path, index=False)

    print(f"Wrote {len(out)} rows to {out_path}")
    print(f"as_of_year: {as_of_year}")
    print(f"regions: {wanted_regions}")
    print(f"sites: {chosen_sites}")


if __name__ == "__main__":
    main()
