# Security Policy

## Supported Versions
This project is maintained on the `main` branch.

## Reporting a Vulnerability
Please report vulnerabilities privately to the repository owner instead of opening a public issue.

When reporting, include:
- affected component (`backend`, `frontend`, CI, docs tooling)
- reproduction steps
- impact and potential exploit path
- suggested remediation if available

## Response Expectations
- Initial triage target: within 5 business days
- High-severity issues are prioritized ahead of feature work
- Fixes are delivered via normal PR workflow with tests and documentation updates

## Security Posture Notes
- Backend dependencies are managed through Maven; frontend through npm.
- Dependency update automation is configured via Dependabot.
- LLM safety boundaries are documented in `docs/05-llm-safety-model.md`.
