# EPIC X.11 – Feature Stability & Drift Explanation

Adds explainable feature drift analysis to complement per-session decisions.

## Backend
- FeatureDriftAnalyzer
- FeatureObservation
- FeatureDriftResult
- DriftLevel

## UI
- FeatureDriftPanel (summary-focused, no charts)

## Notes
- Drift is heuristic and explainability-oriented
- No live wiring or persistence changes
