# Real Snapshot Test Files

These files are small XLSX workbook snapshots derived from real publisher downloads.

They exist to validate ingestion behavior against real-world export artifacts
(BOM, quoting, whitespace, formatting).

These files are NOT full publisher datasets and may be row-trimmed for repository size.

Creation Process:
1. Download publisher workbook (MBIE or LAWA).
2. Open workbook in Excel or LibreOffice.
3. Save the workbook snapshot under this folder.
4. Optionally trim rows, but DO NOT modify headers or column order.

These files are used only for integration-style tests and CLI ingestion verification.

## Size Guidance
Ideal:
- 20 to 100 rows
- Header untouched
- No manual editing

One snapshot per publisher family is sufficient:
- MBIE: one XLSX sample
- LAWA: one XLSX sample

## Expected Contract Outputs
Transformer tests compare XLSX snapshots to contract CSV outputs stored under:
`real_snapshots/expected/`.
