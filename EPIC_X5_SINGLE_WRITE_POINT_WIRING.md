# EPIC X.5 – Single Write-Point Wiring

This EPIC introduces the canonical aggregation application point.

## What is wired
- Explicit aggregation via RiskAggregationEngine
- Single aggregation entry point (AggregatedDecisionApplier)

## What is NOT done
- No controller refactors
- No persistence changes
- No API changes

This prepares the system for safe persistence of explainability data.
