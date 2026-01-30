# Dataset Source Taxonomy Contract

This document defines the canonical naming contract for `dataset_source.code`.

## Purpose

`dataset_source.code` is the **stable, human-readable, machine-parsable identifier**
for datasets across lineage, ingestion, APIs, documentation, and LLM fact packs.

The taxonomy must balance:
- Stability (no forced migrations)
- Discoverability
- Controlled extensibility

---

## Allowed Forms

### v1 (Legacy, Immutable)

<publisher>.<domain>.<variant>

Example:
- mbie.generation.annual
- mbie.generation.quarterly

### v2 (Optional Dataset Segment)

<publisher>.<domain>.<dataset>.<variant>

Example:
- lawa.water_quality.state.multi_year
- lawa.water_quality.site_measurements.daily

---

## Segment Semantics

| Segment   | Meaning | Examples |
|-----------|----------|----------|
| publisher | Stable authority / source organization | mbie, lawa |
| domain | Broad thematic domain | generation, water_quality |
| dataset | Optional sub-family within a domain | state, site_measurements |
| variant | Temporal grain or resolution | annual, quarterly, multi_year, monthly |

---

## Rules (Non-Negotiable)

1. Publisher, domain, dataset, variant **must be lowercase snake_case**.
2. Variant **must encode temporal grain or resolution**, not semantic meaning.
3. Dataset segment is **optional** but must be documented in decisions.md when introduced.
4. Maximum segments allowed: **4**.
    - Any >4 segment form requires a Staff-level architectural decision.
5. Existing v1 codes are **immutable**. No forced renames or migrations.
6. No semantic overloading:
    - ❌ lawa.water_quality.nitrate.multi_year (indicator ≠ dataset)
    - ❌ mbie.generation.wind.annual (fuel_type ≠ dataset)
   - ❌ lawa.water_quality.state_trend.summary (variant must be temporal grain, not meaning)

---

## Rationale

- Preserves backward compatibility for MBIE Phase 6/7 datasets.
- Enables LAWA dataset family grouping without taxonomy hacks.
- Prevents uncontrolled string taxonomy drift.

---

## Enforcement

- Parsers and services must explicitly accept only:
    - 3-segment or 4-segment codes
- Unknown patterns must fail fast or escalate per roles.md.
