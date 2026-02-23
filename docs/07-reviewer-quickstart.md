# Reviewer Quickstart (10 Minutes)

This page is the fastest operational entry point for technical reviewers.

## 1) Start the stack

```bash
docker compose up -d --build
```

Expected:
- `waiwatts-postgres`, `waiwatts-backend`, and `waiwatts-frontend` are running.

## 2) Verify baseline health and scope

```bash
curl -s http://localhost:8080/api/v1/health
curl -s http://localhost:8080/api/v1/capabilities
```

Expected:
- health endpoint returns HTTP 200.
- capabilities endpoint returns supported question types, datasets, and filter structures.

## 3) Verify a supported ask flow

```bash
curl -s -X POST http://localhost:8080/api/v1/explanations/ask \
  -H "Content-Type: application/json" \
  -d '{"question":"Explain renewable generation trends between 2020 and 2023"}'
```

Expected:
- `isRefusal=false` for supported paths with available data.
- response includes citations for non-refusal answers.

## 4) Verify an unsupported ask flow

```bash
curl -s -X POST http://localhost:8080/api/v1/explanations/ask \
  -H "Content-Type: application/json" \
  -d '{"question":"Forecast renewable generation for 2030"}'
```

Expected:
- `isRefusal=true`.
- refusal code is structured and deterministic (for example `UNSUPPORTED_CAPABILITY`).

## 5) Contract invariants to check

- LLM interprets language; backend validates and computes.
- Non-refusal answers include citations that map to Fact Pack facts for the request.
- Capabilities are registry/catalog-driven and discoverable via `GET /api/v1/capabilities`.
- Unsupported/out-of-scope requests refuse deterministically.

## 6) Regression test commands

Backend contract-focused tests:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export PATH="$JAVA_HOME/bin:$PATH"
mvn -pl backend -Dtest=AskEndpointContractIntegrationTest,ExplanationControllerRefusalIntegrationTest,OpenAiIntentParserTest,ExplanationServiceImplEdgeCaseTest test
```

Frontend compatibility test:

```bash
npm --prefix frontend run test:run -- src/test/components.test.tsx
```

