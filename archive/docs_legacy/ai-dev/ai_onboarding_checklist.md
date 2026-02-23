# AI Onboarding Checklist — Wai & Watts

This checklist validates that repository documentation is sufficient to onboard a new AI agent before assigning engineering tasks.

Use this file as an operational validation tool, not as AI instructions.

---

## 1) Core Understanding

- [ ] Agent can summarize the project purpose in ≤3 sentences  
- [ ] Agent correctly identifies completed phases and the current phase  
- [ ] Agent understands the ingestion lifecycle and lineage model  

---

## 2) Repository Navigation

- [ ] Agent knows where fixtures are stored  
- [ ] Agent knows where schema and design docs are located  
- [ ] Agent knows where progress and decisions docs are located  

---

## 3) Conventions & Taxonomy

- [ ] Agent follows dataset taxonomy conventions (e.g., mbie.generation.annual)  
- [ ] Agent follows naming conventions for variants (e.g., MbieGenerationQuarterly)  
- [ ] Agent understands fixture-first ingestion contracts  

---

## 4) Role Discipline

- [ ] Agent understands Staff vs Builder vs PM roles  
- [ ] Agent avoids unsolicited production code generation when asked architectural questions  

---

## 5) Scope Discipline

- [ ] Agent knows forecasting is out of scope  
- [ ] Agent knows real-time dashboards are out of scope  
- [ ] Agent knows live ingestion may be deferred depending on phase  

---

## 6) Failure Handling Procedure

If any checklist item fails:

1. Update documentation (project-context.md, progress.md, decisions.md, or ai-dev docs)  
2. Add missing conventions or clarifications  
3. Re-run onboarding validation questions  
4. Treat onboarding failure as a documentation defect  

---

## 7) Onboarding Completion Criteria

Onboarding is considered complete when all checklist items pass without manual correction prompts.

This process mirrors human developer onboarding validation used in production platform teams.
