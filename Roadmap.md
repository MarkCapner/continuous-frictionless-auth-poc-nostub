
# Continuous Frictionless Authentication — Roadmap

This roadmap describes the **post-EPIC-13 evolution** of the Continuous Frictionless Authentication PoC, reordered **strictly by demo criticality**, with **prompt-ready mini‑EPICs** you can paste directly into ChatGPT or your Dev Agent.

---

## How to Use This Roadmap

Each mini‑EPIC includes a **Prompt** section.
- Prompts are **self-contained**
- Designed to produce a **ZIP update of the existing repo**
- Assume **no stubs, no rewrites, no greenfield**
- Always update the existing codebase

---

# 🔴 EPIC 14 — API Refactor, Package Hygiene & Module Boundaries
**Foundational (Do First)**

### Goal
Reduce complexity in the API layer by introducing clear feature-based package boundaries.

---

## 14.1 API Package Reorganisation

**Prompt**
```
Refactor the API module into feature-based packages without changing behaviour.

Create top-level packages:
- admin
- showcase
- risk
- ml
- identity
- telemetry
- common
- config

Move controllers, services, repositories, and DTOs into their respective feature folders.
Update imports accordingly.
Do not change endpoints or logic.
Return a single ZIP of the updated repo.
```

---

## 14.2 Controller Cleanup & Naming

**Prompt**
```
Rename and regroup controllers for clarity.

Rules:
- Admin controllers must start with Admin*
- Showcase controllers must start with Showcase*
- Risk decision endpoints must live under risk

Remove overloaded controllers.
Keep routes backward compatible.
Return a ZIP.
```

---

## 14.3 DTO & API Contract Isolation

**Prompt**
```
Move all request/response DTOs out of controllers and entities.

Create dto subpackages per feature.
Ensure controllers no longer expose JPA entities.
No behaviour change.
Return a ZIP.
```

---

## 14.4 API Versioning & Route Consistency

**Prompt**
```
Introduce /api/v1 routing.
Align admin vs showcase route namespaces.
Ensure existing UI continues to function.
Return a ZIP.
```

---

# 🔴 EPIC 15 — UX Refactor & Information Architecture

### Goal
Make the system immediately understandable to first-time viewers.

---

## 15.1 Three‑Tier UI Model

**Prompt**
```
Refactor the UI into three tiers:
- Showcase (default)
- Analyst
- Admin / ML Ops

Separate navigation and routes clearly.
No loss of functionality.
Return a ZIP.
```

---

## 15.2 Progressive Disclosure

**Prompt**
```
Replace dense tables with summary cards.
Add expandable detail panels.
Raw JSON must be opt-in.
Return a ZIP.
```

---

## 15.3 Global Session Context

**Prompt**
```
Add a global session selector.
Add a sticky risk summary header.
Ensure it works across all views.
Return a ZIP.
```

---

# 🔴 EPIC 16 — Explainability & Risk Narratives

### Goal
Explain *why* a decision happened.

---

## 16.1 Feature Contribution Engine

**Prompt**
```
Expose top positive and negative feature contributors per session decision.
Store contributions with the session.
Return a ZIP.
```

---

## 16.2 Natural Language Risk Summaries

**Prompt**
```
Generate human-readable risk explanations using feature contributions.
Example: "High risk due to unseen TLS family and abnormal typing cadence."
Return a ZIP.
```

---

## 16.3 Explanation Timeline

**Prompt**
```
Add a per-session explanation timeline in the UI.
Show how evidence accumulated.
Return a ZIP.
```

---

# 🔴 EPIC 17 — Showcase & Demo Experience

### Goal
Deliver a repeatable demo story.

---

## 17.1 Guided Demo Mode

**Prompt**
```
Add a guided demo mode.
Provide step-by-step narrative text.
Do not hide real functionality.
Return a ZIP.
```

---

## 17.2 Scenario Presets

**Prompt**
```
Add demo presets:
- Benign
- Drift
- Attack

Expose them in the UI.
Return a ZIP.
```

---

## 17.3 Demo Reset & Replay

**Prompt**
```
Add demo reset and session replay controls.
Ensure deterministic behaviour.
Return a ZIP.
```

---

# 🔴 EPIC 18 — Synthetic Data & Scenario Injection

### Goal
Guarantee reliable demos and training data.

---

## 18.1 Synthetic Scenario Generator

**Prompt**
```
Implement a synthetic session generator for:
- Benign behaviour
- Gradual drift
- Attack scenarios

Synthetic data must flow through the normal pipeline.
Return a ZIP.
```

---

## 18.2 Scenario Injection API

**Prompt**
```
Expose admin APIs to inject synthetic sessions.
Clearly label synthetic data.
Return a ZIP.
```

---

## 18.3 Synthetic Data Labelling

**Prompt**
```
Tag synthetic sessions in the database.
Surface synthetic indicators in the UI.
Return a ZIP.
```

---

# 🟧 EPIC 19 — Identity Graph & Cross‑Device Intelligence

## 19.1 Identity Confidence Scoring

**Prompt**
```
Compute an identity confidence score per user based on linked devices.
Store and surface the score.
Return a ZIP.
```

---

## 19.2 Device Clustering

**Prompt**
```
Cluster devices as known, related, or suspicious.
Explain why devices are linked.
Return a ZIP.
```

---

## 19.3 Identity Graph Visualisation

**Prompt**
```
Add an interactive identity graph view.
Include "why linked" explanations.
Return a ZIP.
```

---

# 🟧 EPIC 20 — Adaptive Authentication Orchestration

## 20.1 Decision Outcomes

**Prompt**
```
Add allow, step-up, monitor, and block outcomes based on risk.
Return a ZIP.
```

---

## 20.2 Policy Rules Engine

**Prompt**
```
Implement configurable policy rules for decision outcomes.
Policies must be explainable.
Return a ZIP.
```

---

## 20.3 Policy Simulator

**Prompt**
```
Add a policy simulation view.
Replay historical sessions against policies.
Return a ZIP.
```

---

# 🟨 EPIC 21 — Model Lifecycle & Online Learning

## 21.1 Drift‑Triggered Retraining

**Prompt**
```
Trigger retraining on sustained drift signals.
Record model versions.
Return a ZIP.
```

---

## 21.2 Model Comparison

**Prompt**
```
Expose model performance comparisons over time.
Return a ZIP.
```

---

## 21.3 Canary Rollout

**Prompt**
```
Route partial traffic to candidate models.
Auto-rollback on degradation.
Return a ZIP.
```

---

# 🟩 EPIC 22 — Risk Calibration & Evaluation

## 22.1 Threshold Calibration

**Prompt**
```
Add risk threshold tuning controls.
Return a ZIP.
```

---

## 22.2 Performance Metrics

**Prompt**
```
Expose precision, recall, and confusion matrices.
Return a ZIP.
```

---

# 🟩 EPIC 23 — Production Hardening & Privacy

## 23.1 Privacy Controls

**Prompt**
```
Add data retention and hashing controls.
Return a ZIP.
```

---

## 23.2 Telemetry Integrity

**Prompt**
```
Add signed telemetry and replay attack detection.
Return a ZIP.
```

---

# 🟩 EPIC 24 — Multi‑Tenant & SaaS Readiness

## 24.1 Tenant Isolation

**Prompt**
```
Introduce tenant-scoped data, models, and policies.
Return a ZIP.
```

---

## 24.2 Tenant Roles

**Prompt**
```
Add tenant-level admin roles and permissions.
Return a ZIP.
```

---

## Demo‑First Build Order

1. EPIC 14 — API Refactor  
2. EPIC 15 — UX & IA Cleanup  
3. EPIC 16 — Explainability  
4. EPIC 18 — Synthetic Data  
5. EPIC 17 — Demo Experience  

---

*This roadmap is designed to be executed directly, one prompt at a time.*
