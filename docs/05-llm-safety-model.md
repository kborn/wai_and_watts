# LLM Safety Model

LLMs operate exclusively on Fact Pack inputs.

LLMs are prohibited from:
- Querying the database
- Accessing domain entities
- Reading raw publisher artifacts
- Inferring missing facts
- Inventing metrics not present in the Fact Pack

Requirements:
- Explanations must cite Fact Pack fields/IDs.
- Non-refusal explanations must include non-empty citations, and each citation must resolve to a Fact Pack fact ID for that request.
- Unsupported or ambiguous questions trigger deterministic refusal.
- Natural language support performs intent parsing only; it cannot bypass Fact Pack construction.

Safety is enforced by architecture, not by prompt design alone.
