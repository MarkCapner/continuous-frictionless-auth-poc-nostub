
package com.poc.api.risk.explainability.service;

import java.util.List;

import com.poc.api.risk.explainability.dto.*;
import com.poc.api.risk.explainability.domain.*;
import com.poc.api.risk.explainability.baseline.*;

public class SessionExplanationService {

    private final BaselineComparisonService baselineService = new BaselineComparisonService();
    private final DomainContributionAggregator aggregator = new DomainContributionAggregator();

    public SessionExplanationDto explain(String sessionId, List<FeatureContributionInput> inputs) {
        var domains = aggregator.aggregate(inputs).entrySet().stream()
                .map(e -> new DomainExplanationDto(
                        e.getKey(),
                        e.getValue().absoluteScore(),
                        baselineService.compare(e.getValue().absoluteScore(), 0.0, 1.0)
                ))
                .toList();

        return new SessionExplanationDto(sessionId, domains);
    }

    public record FeatureContributionInput(String feature, double contribution) {}
}
