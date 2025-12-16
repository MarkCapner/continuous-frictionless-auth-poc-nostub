EPIC 16.1 – Fixed & Compiling

Changes:
- Restored canonical FeatureContribution model
- Removed AspectJ / AOP usage entirely
- Added ExplainabilityActivator (explicit, safe wiring)
- Attribution wired directly in decision service (where present)

Result:
- Project compiles cleanly
- Explainability persistence is ACTIVE
- No new dependencies required
