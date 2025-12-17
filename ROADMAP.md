# Continuous Frictionless Authentication PoC — Final Roadmap

This roadmap reflects the **actual completed state** of the PoC and the **remaining high-value work**.
All EPICs below are non-overlapping, architecturally sound, and prompt-ready.

---

## ✅ COMPLETED EPICs (Locked)

These EPICs are complete in spirit and implementation and should not be reworked unless requirements change.

- **EPIC 1–8** — Core Frictionless Authentication Foundations  
  Device profiling, behavioural biometrics, risk engine, rules, identity graph, showcase UI.

- **EPIC 9** — TLS Normalisation & Family Extraction  
  TLS fingerprinting, family clustering, drift metrics, admin & showcase views.

- **EPIC 10** — Risk Aggregation & Fusion  
  Feature fusion pipeline, scoring logic, counterfactual groundwork.

- **EPIC 11** — Drift Detection System  
  Device, behaviour, TLS, and feature drift with metrics and dashboards.

- **EPIC 12** — ML Models & Anomaly Detection  
  Isolation Forest, classifiers, model registry, retraining hooks.

- **EPIC 13** — Showcase & Session Explainability (Initial)  
  Session drill-downs, feature diffs, timelines.

- **EPIC 16** — Explainability & Transparency
    - 16.1 Session explainability
    - 16.2.a Cross-session comparison
    - 16.2.b (defined, optional) Trends & volatility
    - 16.2.c Plain-English narratives
    - 16.2.d Admin explainability analytics

> EPIC 16 is **complete and locked** by design.

---

## 🟡 OPTIONAL / NICE-TO-HAVE (Deprioritised)

- **EPIC 14** — UX Streamlining & Tiering
- **EPIC 15** — Demo / Multi-user onboarding enhancements

These do not affect the core technical narrative.

---

EPIC 16.3 — Feature Contribution Explainability (Completion)
Objective

Complete the feature contribution system by transforming raw per-feature scores into explanations that are understandable, comparable, and defensible across sessions, users, and devices.

Scope

Aggregate feature contributions by logical domain (device, TLS, behaviour, identity, ML).

Compare session-level contributions against stored baselines.

Provide historical trend views for contribution patterns.

Deliverables

Domain-level contribution summaries per session.

Baseline vs session contribution comparisons with anomaly highlighting.

Contribution trend history per user and per device (admin-only).

EPIC 17 — Drift Attribution & Root-Cause Analysis
Objective

Explain why drift occurred, not just that it occurred, by attributing drift scores back to concrete feature-level changes and correlating drift across signals.

Scope

Attribute drift events to specific feature changes with quantified impact.

Correlate drift across device, TLS, behavioural, and identity signals.

Present drift explanations as timelines with plain-English summaries.

Deliverables

Feature-level drift attribution per session.

Cross-signal drift correlation summaries.

Drift explanation views in admin and session UIs.

EPIC 18 — Risk Decision Explainability Layer
Objective

Make every risk decision fully defensible by exposing the reasoning chain from signals to scores to policy outcomes.

Scope

Persist decision reasoning snapshots per session.

Represent decisions as structured trees rather than opaque scores.

Support counterfactual analysis to show how decisions would change under different conditions.

Deliverables

Decision reason trees per session.

What-if / counterfactual decision analysis (admin-only).

Decision explainability panels in the showcase UI.

EPIC 19 — ML Governance & Operational Maturity
Objective

Harden the ML lifecycle to production-grade standards without introducing new models or algorithms.

Scope

Track model sensitivity to drift, feature loss, and noise.

Support shadow (challenger) models running alongside active models.

Provide clear comparison views between active and challenger models.

Deliverables

Model sensitivity metrics per version.

Shadow model execution and comparison reporting.

Admin ML governance dashboards.

EPIC 20 — Identity & Trust Evolution
Objective

Explain how trust evolves over time, moving beyond static identity linking to confidence and trust dynamics.

Scope

Introduce a longitudinal trust score per identity.

Apply decay and reinforcement based on observed behaviour.

Decompose identity confidence into contributing signal weights.

Deliverables

Trust score timelines per identity.

Identity confidence decomposition views.

Trust evolution visualisations in admin and session UIs.

EPIC 21 — Showcase & Stakeholder Storytelling
Objective

Transform the PoC into a narrative-driven experience that can be understood by non-technical stakeholders without losing technical depth.

Scope

Create end-to-end session storytelling views.

Present authentication decisions as chronological narratives.

Add guided demo capabilities for walkthroughs and presentations.

Deliverables

Session Story view (observation → classification → drift → risk → decision).

Guided demo mode with contextual explanations.

Collapsible technical detail for deeper inspection.

Recommended Execution Order

EPIC 16.3 — Feature contribution explainability

EPIC 17 — Drift attribution and root-cause analysis

EPIC 18 — Decision explainability

EPIC 21 — Storytelling and demo experience

EPIC 19 and EPIC 20 — Platform maturity and trust evolution
