
package com.poc.api.risk.explainability.baseline;

public record BaselineComparisonResult(
        double zScore,
        double percentile,
        DeviationLabel label
) {}
