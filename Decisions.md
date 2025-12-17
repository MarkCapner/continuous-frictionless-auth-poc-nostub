# Architectural Decisions — Explainability (EPIC 16.x)

This document records key architectural decisions made during the implementation of
the Explainability stack (EPIC 16.x) in the Continuous Frictionless Authentication PoC.

The goal is to preserve intent, avoid regressions, and guide future extensions.

---

## Decision 1 — Explainability is **observational**, not decision-critical

**Decision**  
Explainability must never influence live risk decisions or authentication outcomes.

**Rationale**
- Decisions must remain deterministic and auditable
- Explainability failures must not impact security
- Prevents “explanations controlling policy” anti-pattern

**Implications**
- Explainability is derived *after* a decision
- No feedback loop into scoring, thresholds, or weights
- Safe to disable or replace without affecting auth

---

## Decision 2 — No persistence framework dependency (JPA) in EPIC 16.2

**Decision**  
EPIC 16.2 deliberately avoids JPA, ORM entities, and schema-bound repositories.

**Rationale**
- The baseline stack does not include Spring Data JPA
- Early persistence coupling caused cascading compile failures
- Explainability data is not correctness-critical

**Implications**
- In-memory stores are used for PoC explainability
- Persistence may be added later as a dedicated EPIC
- No Flyway migrations required for EPIC 16.2

---

## Decision 3 — Canonical FeatureContribution model is owned by Risk Engine

**Decision**  
`FeatureContribution` is defined once and shared across:
- risk aggregation
- counterfactuals
- explainability

```java
FeatureContribution(
  String key,
  double rawValue,
  double weight,
  double contribution
)
