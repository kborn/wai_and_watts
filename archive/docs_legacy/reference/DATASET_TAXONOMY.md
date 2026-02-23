# Dataset Taxonomy

Owner: Backend/platform architecture documentation
Last updated: 2026-02-23

`dataset_source.code` follows stable taxonomy naming.

## Active dataset source codes

| Dataset Source Code | Domain Table | API Family | Publisher |
|---|---|---|---|
| `mbie.generation.annual` | `mbie_generation_annual_record` | `/api/v1/mbie/generation/annual` | MBIE |
| `mbie.generation.quarterly` | `mbie_generation_quarterly_record` | `/api/v1/mbie/generation/quarterly` | MBIE |
| `lawa.water_quality.state.multi_year` | `lawa_state_multi_year_record` | `/api/v1/lawa/water-quality/state/multiyear` | LAWA |
| `lawa.water_quality.trend.multi_year` | `lawa_water_quality_trend_multi_year_record` | `/api/v1/lawa/water-quality/trend/multiyear` | LAWA |

## Governance notes
- `dataset_source.code` is the stable runtime lookup identifier.
- URL changes at publisher source do not change dataset identity.
- New dataset codes require:
  - decision entry in `docs/ai-dev/decisions.md`
  - schema/contract update in `design/`
  - progress entry in `docs/ai-dev/progress.md`
