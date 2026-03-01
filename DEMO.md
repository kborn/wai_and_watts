# Wai & Watts Demo (5 Minutes)

This walkthrough shows a quick local demo using the existing API surface.

## 1) Start the stack

Copy the environment template first if you want live LLM calls:

```bash
cp .env.example .env
```

```bash
docker compose up -d --build
```

## 2) Ingest one dataset (fast path)

```bash
docker compose run --rm ingest \
  mbie.generation.annual --bundle-date 2026-02-06 \
  --published-date 2025-11-01 --release-label "MBIE Q3 2025"
```

## 3) Run curl API checks

### Health

```bash
curl "http://localhost:8080/api/v1/health"
```

### MBIE annual rows

```bash
curl "http://localhost:8080/api/v1/mbie/generation/annual?fromYear=2020&toYear=2024"
```

### Dataset sources

```bash
curl "http://localhost:8080/api/v1/datasets/sources"
```

## 4) Grounded explanation example (natural language)

```bash
curl -X POST "http://localhost:8080/api/v1/explanations/ask" \
  -H "Content-Type: application/json" \
  -d '{
    "question": "Explain renewable generation trends between 2020 and 2023"
  }'
```

Expected shape includes:
- `isRefusal`
- `selectedDatasetSource`
- `explanation`
- `citations`

## 5) (Optional) Structured explanation example

```bash
curl -X POST "http://localhost:8080/api/v1/explanations" \
  -H "Content-Type: application/json" \
  -d '{
    "questionType": "renewable_generation_trend",
    "datasetSource": "mbie.generation.annual",
    "filters": {
      "startYear": 2020,
      "endYear": 2023
    }
  }'
```

## 6) Stop services

```bash
docker compose down
```
