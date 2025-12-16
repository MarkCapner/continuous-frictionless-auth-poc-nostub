# Remaining_EPICS.md

> Continuous Frictionless Authentication PoC  
> Remaining work after completion of EPICs 1–9 and partial delivery of 10, 11, 13

## Overview

The core frictionless authentication pipeline is complete:
- Telemetry ingestion
- Device profiling
- Behavioral biometrics
- TLS fingerprinting & family extraction
- Risk scoring & ML
- Admin analytics & showcase UI

The remaining work focuses on:
1. Continuous decisions
2. Drift formalisation
3. Identity convergence
4. Explainable policy enforcement
5. Trust lifecycle hardening

---

## EPIC 12 — Continuous Authentication Engine

### EPIC 12.1 — Session Confidence Model
Prompt:
Implement EPIC 12.1: introduce a continuous session confidence model. Add a confidence score that starts at initial authentication and updates over time. Store confidence snapshots per session with timestamps. Update DB schema, RiskService, and session model. Return a ZIP.

### EPIC 12.2 — Confidence Decay Engine
Prompt:
Implement EPIC 12.2: add time-based confidence decay. Confidence should decay gradually based on inactivity and elapsed time. Make decay parameters configurable. Integrate with the session confidence model. Return a ZIP.

### EPIC 12.3 — Mid-Session Risk Re-Evaluation
Prompt:
Implement EPIC 12.3: re-evaluate risk mid-session when new telemetry arrives. Recompute partial risk and update session confidence without restarting the session. Return a ZIP.

### EPIC 12.4 — Step-Up & Challenge Triggers
Prompt:
Implement EPIC 12.4: introduce step-up and challenge triggers. Add decision outcomes: ALLOW, STEP_UP, CHALLENGE, DENY. Integrate with confidence thresholds. Return a ZIP.

### EPIC 12.5 — Confidence Timeline UI
Prompt:
Implement EPIC 12.5: add a session confidence timeline to the Showcase UI. Show confidence changes, decay, and step-up events. Return a ZIP.

### EPIC 12.6 — Confidence Explanation
Prompt:
Implement EPIC 12.6: expose explanations for confidence changes. Attribute deltas to device, behavior, TLS, drift, or policy causes. Return a ZIP.

---

## EPIC 11 — Drift Detection System

### EPIC 11.1 — Drift Feature Formalisation
Prompt:
Implement EPIC 11.1: formalise drift feature extraction across device, TLS, behavior, and ML features. Persist drift feature vectors. Return a ZIP.

### EPIC 11.2 — Drift Scoring Engine
Prompt:
Implement EPIC 11.2: add drift scoring per dimension. Distinguish gradual vs sharp drift. Return a ZIP.

### EPIC 11.3 — Time Window Baselines
Prompt:
Implement EPIC 11.3: compare short-term (7d) vs long-term (30d) baselines for drift. Return a ZIP.

### EPIC 11.4 — Drift Alerts
Prompt:
Implement EPIC 11.4: add drift thresholds and alerts. Flag sessions and users. Return a ZIP.

### EPIC 11.5 — Drift Timeline UI
Prompt:
Implement EPIC 11.5: add drift timelines to the Admin UI. Return a ZIP.

### EPIC 11.6 — Drift → Risk Integration
Prompt:
Implement EPIC 11.6: feed drift scores into RiskService and continuous auth decisions. Return a ZIP.

---

## EPIC 10 — Identity Graph Completion

### EPIC 10.1 — Graph Traversal & Clustering
Prompt:
Implement EPIC 10.1: build identity graph traversal and clustering logic using weighted links. Return a ZIP.

### EPIC 10.2 — Confidence-Weighted Merges
Prompt:
Implement EPIC 10.2: compute cluster confidence scores and soft merges. Return a ZIP.

### EPIC 10.3 — Conflict Detection
Prompt:
Implement EPIC 10.3: detect and flag conflicting identity signals. Return a ZIP.

### EPIC 10.4 — Identity Graph Admin UI
Prompt:
Implement EPIC 10.4: add Admin UI for identity clusters and links. Return a ZIP.

### EPIC 10.5 — Link Explanations
Prompt:
Implement EPIC 10.5: explain why identities are linked (TLS, behavior, device, account). Return a ZIP.

---

## EPIC 13 — Policy Engine Completion

### EPIC 13.1 — Deterministic Evaluation
Prompt:
Implement EPIC 13.1: enforce deterministic policy evaluation order and conflict resolution. Return a ZIP.

### EPIC 13.2 — Policy Versioning
Prompt:
Implement EPIC 13.2: add versioned policy sets with activate/rollback. Return a ZIP.

### EPIC 13.3 — Policy Audit Trail
Prompt:
Implement EPIC 13.3: persist policy decision traces (which rule fired and why). Return a ZIP.

### EPIC 13.4 — Policy → Risk Binding
Prompt:
Implement EPIC 13.4: bind policy outcomes directly into RiskService decisions. Return a ZIP.

### EPIC 13.5 — Policy Simulation UI
Prompt:
Implement EPIC 13.5: enhance policy simulation UI to replay historical sessions. Return a ZIP.

---

## EPIC 14 — Trust Lifecycle & Hardening

### EPIC 14.1 — Reputation Decay
Prompt:
Implement EPIC 14.1: add reputation decay over time for users and devices. Return a ZIP.

### EPIC 14.2 — Cold-Start Handling
Prompt:
Implement EPIC 14.2: handle new users, devices, and TLS fingerprints gracefully. Return a ZIP.

### EPIC 14.3 — Retention & Privacy Controls
Prompt:
Implement EPIC 14.3: add configurable retention windows and feature redaction. Return a ZIP.

### EPIC 14.4 — ML Lifecycle Enforcement
Prompt:
Implement EPIC 14.4: enforce ML retraining cadence and model expiry. Return a ZIP.

### EPIC 14.5 — Performance Hardening
Prompt:
Implement EPIC 14.5: add performance guards and async processing where needed. Return a ZIP.
