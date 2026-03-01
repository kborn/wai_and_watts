# Ask Observability Runbook

Status: Current Phase 17 operator runbook  
Audience: Engineers and operators diagnosing `/api/v1/explanations/ask`

## Scope

This runbook covers:
- stage-level ask and explanation metrics
- request correlation for ask/explanation flows
- refusal-category triage

This runbook does not cover dashboard delivery or alerting thresholds.

## Correlation Id

- Incoming request headers:
  - `Request-Id`
  - `X-Request-Id` (legacy compatibility)
- Outgoing response headers:
  - `Request-Id`
  - `X-Request-Id`
- Log correlation:
  - log pattern includes `%X{requestId}`
  - request id is populated by `RequestCorrelationFilter`

If a client does not send a request id, the backend generates one and returns it in the response headers.

## Metrics

### Ask metrics

- `waiwatts.ask.stage.count{stage=parse|selection|validation|explanation}`
- `waiwatts.ask.stage.duration{stage=parse|selection|validation|explanation}`
- `waiwatts.ask.refusal.count{stage=...,code=...}`
- `waiwatts.ask.success.count`

### Explanation metrics

- `waiwatts.explanation.stage.count{stage=provider|citation_validation}`
- `waiwatts.explanation.stage.duration{stage=provider|citation_validation}`

## Triage Order

1. Capture the `Request-Id` from the client response.
2. Find all log lines for that request id.
3. Identify the terminal outcome:
   - success
   - refusal
   - exception
4. Check matching counters and stage timings.
5. If the refusal is recurring, compare the tagged refusal counter over recent runs.

## Refusal Map

### `UNSUPPORTED_CAPABILITY`

Likely causes:
- parser mapped the question to an unsupported intent
- validation rejected an unsupported question type
- no compatible dataset/capability path exists for the request

Check:
- `waiwatts.ask.refusal.count{code=UNSUPPORTED_CAPABILITY}`
- parse-stage logs for parser outcome
- capability registry coverage for the requested question type and dataset

### `UNABLE_TO_PARSE`

Likely causes:
- parser could not produce a valid structured request
- malformed or too-ambiguous natural-language input

Check:
- `waiwatts.ask.refusal.count{stage=parse,code=UNABLE_TO_PARSE}`
- parse-stage latency and parser-used debug field

### `MISSING_REQUIRED_FILTERS`

Likely causes:
- NL parse omitted required filters
- request shape was valid but incomplete for the selected capability

Check:
- `waiwatts.ask.refusal.count{stage=validation,code=MISSING_REQUIRED_FILTERS}`
- validation-stage logs
- `/api/v1/capabilities` required filter contract

### `DATASET_MISMATCH`

Likely causes:
- selected dataset does not support the parsed question type
- explicit dataset input conflicts with supported capability matrix

Check:
- `waiwatts.ask.refusal.count{stage=validation,code=DATASET_MISMATCH}`
- selection-stage logs and dataset-selection reason
- capability registry dataset support matrix

### `VALIDATION_FAILED`

Likely causes:
- request body missing `question`
- unsupported filter combinations
- invalid request structure that did not map to a more specific refusal code

Check:
- `waiwatts.ask.refusal.count{code=VALIDATION_FAILED}`
- validation-stage logs
- bad-request responses from non-ask endpoints if the failure occurred before ask-envelope handling

### `NO_DATA`

Likely causes:
- request was valid but the fact pack contained no matching rows
- selected period/filter combination is outside available data coverage

Check:
- `waiwatts.ask.refusal.count{stage=explanation,code=NO_DATA}`
- explanation-stage latency
- builder selection logs
- dataset availability for the requested year, region, indicator, or metric

### `INTERNAL_ERROR`

Likely causes:
- uncaught runtime exception in ask flow
- explanation generator failure
- citation validation failure surfaced as an internal refusal path

Check:
- `waiwatts.ask.refusal.count{code=INTERNAL_ERROR}`
- logs for the matching `Request-Id`
- `waiwatts.explanation.stage.duration{stage=provider}`
- `waiwatts.explanation.stage.duration{stage=citation_validation}`

## Common Patterns

### High parse latency, low refusal count

Interpretation:
- parser/provider dependency is slow but still succeeding

Primary signals:
- `waiwatts.ask.stage.duration{stage=parse}`
- logs grouped by `Request-Id`

### High `NO_DATA` count

Interpretation:
- valid requests are reaching the builder, but dataset coverage or filters are too narrow

Primary signals:
- `waiwatts.ask.refusal.count{stage=explanation,code=NO_DATA}`
- selection reason and parsed filters in ask response debug payload

### High `INTERNAL_ERROR` count with long citation validation duration

Interpretation:
- explanation generation may be succeeding, but citation integrity is failing late

Primary signals:
- `waiwatts.ask.refusal.count{code=INTERNAL_ERROR}`
- `waiwatts.explanation.stage.duration{stage=citation_validation}`
- request-id-linked exception logs
