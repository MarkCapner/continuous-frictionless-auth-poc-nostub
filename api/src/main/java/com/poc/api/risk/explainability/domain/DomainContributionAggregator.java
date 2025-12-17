
package com.poc.api.risk.explainability.domain;

import java.util.*;
import java.util.stream.Collectors;

public class DomainContributionAggregator {

    private final FeatureDomainResolver resolver = new DefaultFeatureDomainResolver();

    public Map<FeatureDomain, DomainContribution> aggregate(
            List<FeatureContributionInput> inputs
    ) {
        Map<FeatureDomain, Double> signed = new HashMap<>();
        Map<FeatureDomain, Double> abs = new HashMap<>();

        for (FeatureContributionInput in : inputs) {
            FeatureDomain d = resolver.resolve(in.feature());
            signed.merge(d, in.contribution(), Double::sum);
            abs.merge(d, Math.abs(in.contribution()), Double::sum);
        }

        return signed.keySet().stream()
                .collect(Collectors.toMap(
                        d -> d,
                        d -> new DomainContribution(
                                signed.getOrDefault(d, 0.0),
                                abs.getOrDefault(d, 0.0)
                        )
                ));
    }

    public record FeatureContributionInput(String feature, double contribution) {}
}
