🚀 Continuous Frictionless Authentication — Proof of Concept
Device Profiling • TLS Fingerprinting • Behavioral Biometrics • Adaptive ML • Continuous Authentication
4
❤️ Project Badges














⭐ Overview

This repository contains a complete, end-to-end Frictionless Continuous Authentication System, demonstrating:

✔ Passive device profiling
✔ TLS JA4-style fingerprinting from ClientHello
✔ Lightweight, privacy-preserving behavioral biometrics
✔ Adaptive ML scoring with anomaly detection
✔ Account-sharing & multi-device heuristics
✔ Deterministic rules engine (impossible travel, VPN, new device risk, etc.)
✔ Full React-based showcase UI visualizing all signals
✔ Zero cookies, zero storage, zero PII

The PoC highlights how modern identity systems fuse invisible signals to determine trust without explicit authentication steps.

🏗 Architecture Overview
High-Level Components
Component	Description
React Showcase UI	Collects device & behavior telemetry; visualizes scores, decisions, timelines
TLS Gateway (Netty + BoringSSL)	TLS termination, JA4 fingerprint extraction, proxy to API
Risk & Auth API (Spring Boot)	Feature building, ML scoring, anomaly detection, rules engine, persistence
PostgreSQL 18	Device profiles, behavior stats, session features, model registry
📡 Signal Collection Pipeline
1) Browser Profiler SDK

Collects device + behavior telemetry:

UA + UA-CH

CPU cores, memory

Resolution & pixel ratio

Timezone, languages

Canvas/WebGL hashes

Mouse movement stats

Typing cadence (keypress intervals)

Scroll behavior

NO raw keystrokes, NO PII, NO storage of any kind.

2) TLS Gateway

Builds a JA4-style fingerprint using:

TLS version

Cipher suites

Extensions (normalized, GREASE removed)

ALPN

Key shares

SNI

Injected into upstream request as:

X-TLS-FP: <ja4-hash>
X-TLS-Meta: <metadata>

3) Risk API

Performs:

Device similarity

Behavioral similarity

TLS matching

Context scoring

ML prediction (Tribuo)

Anomaly detection (C1)

Multi-device account heuristics (C2)

Deterministic rule overrides

4) Showcase UI

Visualizes everything:

Device card

TLS fingerprint panel

Behavior charts

ML scoring breakdown

Anomaly score

Explanations

Session timeline

🔬 Machine Learning Components
✔ Classifier (Tribuo)

Predicts pLegit based on structured features:

device_score

behavior_score

tls_score

context_score

✔ Anomaly Score (C1)

anomaly_score = 1 - behavior_similarity
Penalizes ML probability by up to 50%.

Shown in Breakdown Panel.

✔ Multi-Device Heuristics (C2)

Flags account-sharing if:

many distinct TLS fingerprints

many distinct countries

Triggers:

STEP_UP override

Explanation in UI

Logged in decision timeline

🎛 Rules Engine

Deterministic risk overrides:

Rule	Behavior
Impossible travel	Force DENY
VPN + New device	STEP_UP
New TLS FP on high-risk action	STEP_UP
Account sharing heuristic	STEP_UP
Threshold-based ML decision	AUTO_LOGIN / STEP_UP / DENY
🗄 Database Schema

Includes:

device_profile

behavior_profile_stats

session_feature

decision_log

model_registry

Supports:

rolling stats

model hot reload

auditing & analytics

🎨 Showcase UI (React 19 + Vite)
Panels Demonstrated

Device Profile

TLS Fingerprint

Behavior Metrics Chart

ML Breakdown (device, behavior, tls, context, anomaly)

Explanations panel

Session Timeline (from DB)

Chaos Mode toggles:

Simulate VPN

High-risk action

Force night-time

Override country

Perfect for stakeholder demonstrations.

🔐 Privacy & Compliance
Zero PII

No names, emails, addresses, or external identifiers.

Zero raw behavioral data

Only aggregates.

Zero client storage

No cookies
No localStorage
No fingerprints persisted locally

GDPR-aligned opt-out

Telemetry is minimized or dropped when opt-out is signaled.

🚀 Running the PoC
# Build API + Gateway
cd api
mvn clean package
cd ../gateway
mvn clean package

# Run via Docker Compose
cd ../infra
docker compose up --build

# Run the UI
cd ../showcase
npm install
npm run dev


Navigate to:

Gateway: https://localhost:8443

Showcase UI: http://localhost:5173

📈 Example Decision Response
{
"decision": "AUTO_LOGIN",
"confidence": 0.92,
"breakdown": {
"device_score": 0.88,
"behavior_score": 0.79,
"tls_score": 0.95,
"context_score": 0.70,
"anomaly_score": 0.12
},
"explanations": [
"ML + rules engine decision",
"TLS FP exact match"
],
"session_id": "abc123",
"tls_fp": "9fa6....",
"tls_meta": "tls=true;sessionId=present;peerCerts=1"
}

🧭 Roadmap & Recommended Improvements

(All the enhancements we’ve discussed so far, consolidated)

Machine Learning Enhancements

Add real Tribuo IsolationForest model

Add GBTs or LogReg trained on real dataset

Online learning with incremental updates

Drift detection for device & behavior vectors

Calibration for confidence stability

Rules & Heuristics

Velocity-based impossible travel

Risk scoring based on fingerprint drift

Behavioral time-of-day models

Bot-like behavior detection patterns

TLS Fingerprinting

Full JA4 canonicalization

JA3/JA4 comparison matrix

TLS drift scoring

Frontend / Showcase

Mouse movement heatmaps

Real-time WebSocket scoring

Device diff inspector panel

More chaos toggles (timezone jumps, resolution shifts, network changes)

Backend & Infra

Prometheus metrics & Grafana dashboards

Redis caching for profiles

Feature store abstraction

Authentication layer integration (OpenID / OAuth)

CI pipeline for model validation

🎉 Final Note

This PoC is no longer just a demo — it is a complete identity intelligence engine that showcases the future of frictionless authentication:

Passive

Real-time

Privacy-preserving

ML-powered

Transparent through a compelling UI