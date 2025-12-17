
package com.poc.api.risk.explainability.dto;

import com.poc.api.risk.explainability.domain.FeatureDomain;
import com.poc.api.risk.explainability.baseline.BaselineComparisonResult;

public record DomainExplanationDto(
        FeatureDomain domain,
        double score,
        BaselineComparisonResult baseline
) {}
