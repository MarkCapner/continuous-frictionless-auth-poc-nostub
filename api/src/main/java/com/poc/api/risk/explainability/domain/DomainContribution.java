
package com.poc.api.risk.explainability.domain;

public record DomainContribution(
        double signedScore,
        double absoluteScore
) {}
