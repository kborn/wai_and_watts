## Archived Python Tooling (Optional)

This folder contains retroactive setup support for archived Python scripts used in historical
Phase 15 analysis artifacts.

These tools are **not required** to run the backend/frontend or Docker ingestion pipeline.

### Setup (one-time)

Requirements: `python3` available on PATH.

```bash
cd archive/tools/python
make python-setup
```

### Activate

```bash
source .venv/bin/activate
```

### Example archived scripts

```bash
python3 ../../phase_notes/phase15/pattern_panel_runner.py --help
python3 ../../phase_notes/phase15/pattern_panel_evaluate.py --help
```
