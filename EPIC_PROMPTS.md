🧩 EPIC 1 — Core Telemetry & Risk Pipeline
Prompt:

“Implement EPIC 1 completely: end-to-end telemetry → feature builder → risk engine → decision logging.
Use the existing repo structure.
Return a ZIP with updated API, feature extraction, RiskService, and DB schema. No placeholders.”

🧩 EPIC 2 — Device Profiling Enhancements
Prompt:

“Implement EPIC 2: advanced device profiling (UA-CH, canvas hash, WebGL hash, timezone drift, device consistency scoring).
Update both frontend telemetry and backend feature extraction.
Return a ZIP.”

🧩 EPIC 3 — TLS Fingerprinting Integration
Prompt:

“Implement EPIC 3: TLS fingerprint (JA3/JA4), gateway capture, risk weighting, and device history tracking.
Add TLS FP to the session feature store and risk breakdown.
Return a ZIP.”

🧩 EPIC 4 — Behavioral Biometrics

(You already have basic keystroke/mouse capture — EPIC 4 expands this into a true statistical/ML signal.)

Prompt:

“Implement EPIC 4: behavioral biometrics including running-stats model, keystroke timing features, scroll cadence, pointer velocity segments, and per-user behavioral baselines.
Update RiskService and DB schema.
Return a ZIP.”

🧩 EPIC 5 — ML Model Integration (Tribuo)
Prompt:

“Implement EPIC 5: Tribuo-based ML model including feature vector schema, model registry, training pipeline, Isolation Forest anomaly score, and integration into RiskService.
Return a ZIP with all updated modules.”

🧩 EPIC 6 — User-Level Intelligence

(You already partially implemented Account Sharing Heuristics.)

Prompt:

“Implement EPIC 6: user-level intelligence including account sharing heuristics, device trust scoring, cross-device correlation, and user reputation service.
Integrate into decision output and update the repo.
Return a ZIP.”

🧩 EPIC 7 — Admin Dashboard API
Prompt:

“Implement EPIC 7: admin APIs for user summaries, device summaries, session analytics, model performance, and risk statistics.
Add Postgres views/materialized views where needed.
Return a ZIP.”

🧩 EPIC 8 — Showcase UI Enhancements

(You already implemented part of this: user selector, typing area, user overview.)

Prompt:

“Implement EPIC 8: full showcase UI dashboard including device diff viewer, behavioral inspector, risk explanation timeline, user switcher, device history charts, and TLS FP visualizer.
Return a ZIP of updated frontend and backend.”

🧩 EPIC 9 — Enterprise Mode (Policy Engine & Multi-Tenant)
Prompt:

“Implement EPIC 9: enterprise features including authentication policy engine, step-up MFA triggers, multi-tenant DB schema (tenant_id), row-level isolation, and tenant-specific model registry.
Return a ZIP.”

🧩 EPIC 10 — Autonomous ML Deployment Agent
Prompt:

“Implement EPIC 10: autonomous ML deployment and update agent.
The agent should:
– detect model drift
– retrain if thresholds exceeded
– version models
– log metrics
– expose admin hooks
Integrate with existing registry.
Return a ZIP and full documentation.”

🧩 EPIC 11 — Drift Detection System
Prompt:

“Implement EPIC 11: drift detection engine for device drift, behavior drift, TLS FP drift, and model feature drift.
Add metrics, dashboards, and warnings.
Return a ZIP.”

🧩 EPIC 12 — Threat Analytics & Alerts
Prompt:

“Implement EPIC 12: threat analytics including anomaly bursts, geographic anomalies, multi-session correlation, and alerting pipeline.
Add admin endpoints and a simple UI.
Return a ZIP.”

🧩 EPIC 13 — Multi-Device Correlation Engine
Prompt:

“Implement EPIC 13: multi-device correlation including cluster analysis, cross-user device mapping, similarity scoring, and fraud linkage maps.
Integrate results into risk scoring.
Return a ZIP.”

🧩 EPIC 14 — Full MFA Step-Up Flow (Optional)
Prompt:

“Implement EPIC 14: MFA step-up flow triggered by high-risk score.
Add temporary challenge tokens, UI prompts, and policy enforcement.
Return a ZIP.”

🧩 EPIC 15 — Hardening, Logging & Observability
Prompt:

“Implement EPIC 15: full hardening & observability including structured logs, OpenTelemetry traces, metrics endpoints, and rate limiting.
Return a ZIP.”