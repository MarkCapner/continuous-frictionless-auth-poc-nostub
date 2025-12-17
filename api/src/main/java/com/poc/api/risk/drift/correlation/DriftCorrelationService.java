
package com.poc.api.risk.drift.correlation;

import java.util.*;

public class DriftCorrelationService {

    public CorrelatedDriftSummary correlate(List<FeatureImpact> impacts) {
        Map<String, Double> signalTotals = new HashMap<>();
        for (FeatureImpact i : impacts) {
            signalTotals.merge(i.signal(), Math.abs(i.impact()), Double::sum);
        }
        return new CorrelatedDriftSummary(signalTotals);
    }

    public record FeatureImpact(String feature, String signal, double impact) {}
    public record CorrelatedDriftSummary(Map<String, Double> signalTotals) {}
}
