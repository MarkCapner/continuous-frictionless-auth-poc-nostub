# 📍 Roadmap.md
**Continuous Frictionless Authentication PoC**  
_Device • Behaviour • TLS • ML • Drift-aware Risk_

## Overview
This roadmap tracks the delivery of the **Continuous Frictionless Authentication PoC** aligned to **SPEC-001**.
The project is structured into **EPICs**, each delivering a coherent, demo-ready capability.

- **EPICs 1–8 are COMPLETE**
- **EPICs 9–14 extend the system into drift-aware, enterprise-grade intelligence**
- Each EPIC is independently deliverable and testable
- Each EPIC can be prompted explicitly for generation and returned as a ZIP

---

## ✅ EPICs 1–8 (Completed)

### EPIC 1 — Core Telemetry & Ingestion
Raw browser, device, TLS, and behavioural telemetry ingestion.

### EPIC 2 — Device Profiling & Persistence
Stable device profiles, drift metrics, and history.

### EPIC 3 — TLS Fingerprinting (Raw)
JA3 / JA4-style TLS fingerprint capture and persistence.

### EPIC 4 — Behavioural Biometrics
Keystroke, pointer, scroll cadence with running stats & z-scores.

### EPIC 5 — ML Risk Engine
Tribuo-based ML, Isolation Forest anomaly scoring, model registry.

### EPIC 6 — User-Level Intelligence
User reputation, account sharing heuristics, cross-device correlation.

### EPIC 7 — Admin Analytics APIs
User, device, session, model, and risk analytics via Postgres views.

### EPIC 8 — Showcase UI Dashboard
Full demo UI: device inspector, behaviour inspector, risk timeline, TLS views, user switcher.

---

## 🔜 EPICs 9–14 (Forward Roadmap)

### EPIC 9 — TLS Normalisation & Family Extraction
Stabilise raw TLS fingerprints into **TLS families** representing browser/TLS stacks.

**Prompt**
```
Deliver EPIC 9: TLS fingerprint normalisation and family extraction.
Implement TLS parsing, GREASE filtering, TLS family IDs, persistence, and backfill.
Return a ZIP with updated backend modules.
```

---

### EPIC 10 — TLS Drift Scoring & Risk Fusion
Introduce **context-aware TLS drift scoring** integrated into RiskService.

**Prompt**
```
Deliver EPIC 10: TLS drift scoring and RiskService integration.
Add TLS drift scorer, rarity metrics, expected drift modelling, and risk fusion.
Expose explainability fields in decision output.
Return a ZIP.
```

---

### EPIC 11 — Unified Drift Detection Framework
Cross-signal drift detection across device, behaviour, TLS, and model features.

**Prompt**
```
Deliver EPIC 11: unified drift detection framework.
Implement drift detection across device, behaviour, TLS, and model features.
Add metrics, dashboards, and warnings.
Return a ZIP.
```

---

### EPIC 12 — Drift & TLS Visualisation (UI)
Make drift understandable via UI visualisation.

**Prompt**
```
Deliver EPIC 12: drift and TLS visualisation UI.
Add TLS diff viewer, drift timelines, and explainability panels to the showcase and admin UI.
Return a ZIP with frontend and backend updates.
```

---

### EPIC 13 — Advanced Clustering & Anomaly Detection
Detect structural anomalies beyond simple drift.

**Prompt**
```
Deliver EPIC 13: advanced clustering and anomaly detection.
Implement clustering for TLS families, devices, and behaviour with anomaly scoring.
Integrate into risk and analytics.
Return a ZIP.
```

---

### EPIC 14 — Policy, Controls & Governance
Enterprise tuning, governance, and explainability controls.

**Prompt**
```
Deliver EPIC 14: policy, controls, and governance.
Add configurable thresholds, signal weight controls, explainability templates, and audit metadata.
Return a ZIP.
```

---

## 📊 EPIC Summary Table

| EPIC | Name | Status | Demo Value | Platform Value |
|-----|------|--------|------------|----------------|
| 1 | Telemetry & Ingestion | Done | High | High |
| 2 | Device Profiling | Done | High | High |
| 3 | TLS Fingerprinting | Done | Medium | High |
| 4 | Behavioural Biometrics | Done | High | High |
| 5 | ML Risk Engine | Done | High | High |
| 6 | User Intelligence | Done | High | High |
| 7 | Admin Analytics | Done | Medium | High |
| 8 | Showcase UI | Done | Very High | Medium |
| 9 | TLS Families | Planned | High | High |
| 10 | TLS Drift Scoring | Planned | High | High |
| 11 | Unified Drift Detection | Planned | Medium | Very High |
| 12 | Drift Visualisation | Planned | Very High | Medium |
| 13 | Advanced Clustering | Planned | Medium | High |
| 14 | Policy & Governance | Planned | Low | Very High |

---

## 🎯 MoSCoW Prioritisation

**Must Have**
- EPIC 1–8
- EPIC 9
- EPIC 10

**Should Have**
- EPIC 11
- EPIC 12

**Could Have**
- EPIC 13

**Won’t Have (PoC Phase)**
- EPIC 14
