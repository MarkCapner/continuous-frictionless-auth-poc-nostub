# EPIC X.7 – Pipeline Wiring

Introduces the canonical RiskDecisionPipeline which wires together:

DecisionAttributionBuilder
→ RiskAggregationEngine
→ DecisionPersistenceAdapter

## Characteristics
- Single orchestration point
- No controller changes
- No implicit behavior changes
- Ready for injection where decisions are created

## Activation
To activate explainability, call RiskDecisionPipeline.apply(...)
from the decision creation path.
