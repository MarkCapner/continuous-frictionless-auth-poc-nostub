🧭 Overview

This roadmap describes the future development direction for the Continuous Frictionless Authentication PoC, organized into EPICs, User Stories, and Tasks suitable for Jira, Linear, Azure DevOps, or autonomous agent pipelines.

All enhancements derive from:

SPEC-001

Completed implementation (baseline repo)

ML upgrades (Isolation Forest, GBT, drift detection)

TLS fingerprint advances

Showcase UI improvements

Infrastructure, observability, and privacy requirements

🟣 EPIC 1 — Machine Learning Evolution & Risk Modeling
Story 1.1 — Isolation Forest Productionization (C1 Upgrade)

Introduce a trained Isolation Forest model for anomaly scoring to replace the heuristic implementation.

Tasks

1.1.1 Add tribuo-anomaly Isolation Forest dataset builder

1.1.2 Extract training data from session_feature

1.1.3 Train Isolation Forest offline

1.1.4 Store serialized model in model_registry

1.1.5 Extend ModelProvider to load anomaly model in parallel

1.1.6 Combine anomaly score with ML probability

1.1.7 Expose anomaly score to UI breakdown

1.1.8 Add test suite for anomaly inference paths

Story 1.2 — Gradient Boosted Trees (GBT) Classifier

Improve accuracy by replacing or complementing logistic regression.

Tasks

1.2.1 Add trainer for GBT via Tribuo

1.2.2 Train on enhanced feature vector

1.2.3 Support hot-reload of multiple model types

1.2.4 Expose model metadata in decision response

1.2.5 Add probability calibration

Story 1.3 — Online Learning (FR-11)

Enable partially online risk model adaptation.

Tasks

1.3.1 Expand time-decayed behavior stats integration

1.3.2 Add online-updatable model type (SGD)

1.3.3 Track rolling drift windows

1.3.4 Monitor model drift & trigger retraining

1.3.5 Add nightly batch retraining job

Story 1.4 — Drift Detection & Visualization

Detect and interpret device drift and behavioral drift.

Tasks

1.4.1 Implement cosine/KL divergence metrics

1.4.2 Store drift deltas in session_feature

1.4.3 Add drift visualization card to UI

1.4.4 Integrate drift into risk scoring

🔵 EPIC 2 — Device & Behavioral Profiling Enhancements
Story 2.1 — Device Drift & Version Change Detection

Detect meaningful device evolution.

Tasks

2.1.1 Add semantic UA comparison

2.1.2 Compute weighted device drift delta

2.1.3 Persist device drift events

2.1.4 Display drift warnings in Device Panel

Story 2.2 — Behavioral Biometrics Heatmaps & Advanced Metrics

Increase behavioral expressiveness.

Tasks

2.2.1 Add mouse velocity sampling

2.2.2 Implement client-side heatmaps

2.2.3 Add interval histogram for keypress cadence

2.2.4 Add behavior anomaly insights

Story 2.3 — Multi-Device Account Sharing (C2 Upgrade)

Enhance heuristics beyond simple thresholds.

Tasks

2.3.1 Compute device diversity per user

2.3.2 Profile country distribution over time

2.3.3 Add sharing likelihood score

2.3.4 Add UI account-sharing notifier

🟠 EPIC 3 — TLS Fingerprinting Enhancements
Story 3.1 — Full JA4 Canonicalization Engine

Improve TLS FP robustness.

Tasks

3.1.1 Implement GREASE removal

3.1.2 Normalize extension ordering

3.1.3 Encode ALPN & SNI canonical forms

3.1.4 Enrich fingerprint metadata in API response

3.1.5 Validate against public JA4 test sets

Story 3.2 — TLS Drift & Multi-Session Consistency

Detect shifts in TLS signature across visits.

Tasks

3.2.1 Compare TLS FP sequences per user

3.2.2 Track drift severity

3.2.3 Integrate drift penalty into risk score

3.2.4 Visualize drift in TLS panel

Story 3.3 — TLS Intelligence Dashboard

Present insights about TLS fingerprints in aggregate.

Tasks

3.3.1 Display browser TLS distribution

3.3.2 Device TLS evolution timeline

3.3.3 Detect known bot TLS fingerprints

🟡 EPIC 4 — Risk Engine & Decision Framework
Story 4.1 — Expand Rules Engine

Increase depth of deterministic risk logic.

Tasks

4.1.1 Add geolocation-based impossible travel

4.1.2 VPN/proxy heuristics based on IP ranges

4.1.3 Risk-category framework (low/med/high)

4.1.4 Webhook-based decision alerting

Story 4.2 — Contextual Intelligence (FR-10)

Improve contextual understanding of the session.

Tasks

4.2.1 Add ASN lookup support

4.2.2 Compute IP stability index

4.2.3 Add hour-of-day sin/cos features

4.2.4 Weekend/holiday flags

🟢 EPIC 5 — Frontend / Showcase Experience (FR-8)
Story 5.1 — Real-Time Streaming Scoring

Replace polling with WebSocket updates.

Tasks

5.1.1 Gateway WS endpoint

5.1.2 API scoring push events

5.1.3 UI subscription layer

5.1.4 Animated score transitions

Story 5.2 — Device Inspector Panel

Deep-diff fingerprint evolution.

Tasks

5.2.1 Visual field-level diff of device profiles

5.2.2 Drift coloring (green/amber/red)

5.2.3 Historical version viewer

Story 5.3 — Risk Timeline Analytics

Give a complete picture of user session history.

Tasks

5.3.1 Anomaly sparkline chart

5.3.2 Raw feature vector preview (safe subset only)

5.3.3 Confidence band visualization

🟤 EPIC 6 — Observability, Logging & Performance (NFR-3)
Story 6.1 — Prometheus Metrics Integration
Tasks

6.1.1 p95 / p99 latency metrics

6.1.2 TLS parsing metrics

6.1.3 ML inference duration histogram

6.1.4 Prometheus scrape config

Story 6.2 — Distributed Tracing Integration

End-to-end visibility across gateway → API → DB.

Tasks

6.2.1 Add OpenTelemetry SDk

6.2.2 Add span creation around fingerprinting, feature building, inference, DB writes

6.2.3 Propagate traceparent headers

6.2.4 Integrate with Jaeger/Zipkin

Story 6.3 — Performance Testing & Optimization

Validate and tune performance to NFR-2.

Tasks

6.3.1 Create k6/Gatling load tests

6.3.2 Validate 200 req/s (p95 <120ms)

6.3.3 Add caching for device profile lookup

6.3.4 Tune DB indexes and connection pooling

🟣 EPIC 7 — Privacy, Security & Compliance
Story 7.1 — GDPR Retention & Opt-Out
Tasks

7.1.1 Background retention job

7.1.2 FE toggle for consent/opt-out

7.1.3 DB flags for non-training sessions

7.1.4 Privacy action logging

Story 7.2 — Security Hardening
Tasks

7.2.1 Add strict CSP headers

7.2.2 Add SRI hash enforcement

7.2.3 Rate limiting and anti-abuse detection

7.2.4 Automated TLS certificate renewal

🟢 EPIC 8 — Infrastructure & DevOps
Story 8.1 — CI/CD Pipeline Automation
Tasks

8.1.1 GitHub Actions build/test pipeline

8.1.2 Docker image publishing

8.1.3 Model integrity validation in CI

8.1.4 Deployment profiles for dev/stage/prod

Story 8.2 — Feature Store Integration
Tasks

8.2.1 Abstract FeatureBuilder into provider interface

8.2.2 Add Redis/DuckDB store for features

8.2.3 Warm caching for device and behavior profiles

8.2.4 DB fallback path

🟩 EPIC 9 — Productization & Enterprise Features
Story 9.1 — OIDC / OAuth2 Integration
Tasks

9.1.1 Provide frictionless risk signals to auth-server

9.1.2 Policy engine for authentication strength

9.1.3 Enable real step-up (MFA) for high-risk sessions

Story 9.2 — Multi-Tenant SaaS Mode
Tasks

9.2.1 Add tenant_id across schema

9.2.2 Row-level isolation rules

9.2.3 Tenant-specific model registry

9.2.4 Cross-tenant analytics prevention