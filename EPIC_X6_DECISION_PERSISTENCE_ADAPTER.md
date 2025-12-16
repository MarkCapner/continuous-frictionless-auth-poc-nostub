# EPIC X.6 – Decision Persistence Adapter

Introduces a canonical adapter for persisting aggregated risk results
and explainability data.

## What this provides
- DecisionPersistenceAdapter
- DecisionWritable contract

## What this does NOT do
- No wiring to existing repositories
- No controller changes
- No schema changes

This allows safe, incremental adoption without guessing persistence internals.
