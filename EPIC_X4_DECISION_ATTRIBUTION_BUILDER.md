# EPIC X.4 – DecisionAttributionBuilder

Introduces a bridge between existing score computations and the
explicit risk aggregation pipeline.

## Responsibilities
- Convert already-computed scores into RiskSignal objects
- Preserve feature naming and provenance
- Perform NO aggregation
- Perform NO persistence
- Perform NO decision logic

## Why this exists
This isolates attribution concerns from aggregation concerns and
keeps the system refactor-safe.
