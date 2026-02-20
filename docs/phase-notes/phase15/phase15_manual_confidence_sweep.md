
# Phase 15 — Manual Confidence Sweep (Human Validation Procedure)

## Purpose

This document defines a **small, repeatable manual validation pass** used to confirm
Phase 15 readiness from a **product trust and UX perspective**, not coverage or determinism.

Automated validation is handled by:
- Pattern Panel Harness
- Pattern Panel Evaluator
- Unit + Integration tests

This sweep validates:
- Narrative quality
- Boundary honesty
- Ambiguity handling
- Demo readiness

---

## When To Run This

Run the Manual Confidence Sweep:

✅ Before declaring Phase 15 complete  
✅ Before portfolio demo recording  
✅ After major Ask pipeline logic changes  
✅ After LLM prompt or provider changes  

Do NOT run this for:
❌ Every PR  
❌ Minor UI changes  
❌ Dataset fixture updates  

---

## Environment Setup

1. Backend running
2. Frontend running (if applicable)
3. LLM configured and reachable
4. Known-good dataset fixtures loaded

Optional but recommended:
- Enable debug logging for:
  - Intent parsing
  - Dataset selection
  - Fact pack construction
  - Citation validation

---

# Manual Validation Zones

You will ask ~20–25 total questions across 4 zones.

This is NOT scored.
This is NOT pass/fail per question.

This is a **system trust check**.

---

# Zone 1 — Supported Shape Reality Check

## Goal
Verify answers feel correct, grounded, and human-trustworthy.

## Ask 6–8 questions

### Trend
- Explain hydro trends since 2000
- How has wind generation changed over the last decade?

### Overview
- What are New Zealand’s main electricity generation sources today?

### Compare
- Compare geothermal vs hydro generation patterns

### LAWA Distribution
- What does river water quality look like across New Zealand?

## Validate

Answers should:
✅ Use real facts  
✅ Include citations  
✅ Use correct dataset  
✅ Sound like something you would say in a demo  

Answers should NOT:
❌ Overstate certainty  
❌ Answer outside evidence  
❌ Use wrong dataset  

---

# Zone 2 — Boundary Honesty (MOST IMPORTANT)

## Goal
Ensure system never fakes capability.

## Ask 6–8 questions

### Derived Analytics
- Which fuel has grown the most since 2005?
- When did solar grow fastest?

### Prediction
- Will solar overtake wind?

### Policy
- Should NZ invest more in geothermal?

### Causation
- Why did hydro drop in 2012?

## Validate

System should:
✅ Refuse cleanly  
✅ Provide correct refusal category  
✅ Not attempt partial answers  

System must NEVER:
❌ Answer a different question instead  
❌ Provide speculative explanation  
❌ Fabricate supporting data  

If this fails → Phase 15 is NOT complete.

---

# Zone 3 — Ambiguity Handling

## Goal
Ensure ambiguous prompts do not cause hallucinated specificity.

## Ask 4–5 questions

Examples:
- How is solar doing?
- Tell me about renewables recently
- What’s happening with river quality?
- Are rivers getting better or worse?

## Validate

Acceptable:
✅ Clarification request  
✅ Reasonable broad overview  
✅ Explicit limitation statement  

Not acceptable:
❌ Fake precision  
❌ Unsupported specific claims  

---

# Zone 4 — Demo Stress Test (Human Phrasing)

## Goal
Ensure system handles real-world messy phrasing.

## Ask 4–5 questions

Examples:
- Are renewables kinda dominating now?
- Is hydro still the main thing or not really?
- Is water quality getting worse overall?

## Validate

System should:
✅ Stay grounded  
✅ Stay calm and descriptive  
✅ Not over-technical  
✅ Not overconfident  

---

# Final Human Signoff Check

After the sweep, ask yourself:

## Trust Question

"Can I make this system lie, hallucinate, or fake capability?"

If NO → Phase 15 manual confidence = PASS  
If YES → Investigate and fix  

---

# Expected Failure Modes (Acceptable in Phase 15)

These do NOT block Phase 15:

⚠ Slightly generic narrative  
⚠ Conservative refusal for borderline questions  
⚠ Asking for clarification when technically not required  

---

# Failure Modes That DO Block Phase 15

🚨 Answering derived analytics instead of refusing  
🚨 Missing citations in answers  
🚨 INTERNAL_ERROR visible to user  
🚨 Answering outside dataset evidence  

---

# Duration

Typical time:
20–30 minutes total

---

# Output (Optional but Recommended)

Record:
- Date
- Version / commit
- Pass / Needs Investigation
- Any notable edge cases discovered

---

# Phase 15 Completion Statement

If:
- Pattern harness passes
- Evaluator passes
- Manual Confidence Sweep passes

Then Phase 15 is complete.

