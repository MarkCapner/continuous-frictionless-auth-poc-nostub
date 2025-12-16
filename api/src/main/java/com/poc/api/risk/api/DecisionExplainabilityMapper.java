package com.poc.api.risk.api;

import com.poc.api.risk.api.dto.DecisionExplainabilityDto;

public class DecisionExplainabilityMapper {

  public static DecisionExplainabilityDto toDto(DecisionExplainable d) {
    return new DecisionExplainabilityDto(
        d.getDecision(),
        d.getConfidence(),
        d.getFeatureContributionsJson(),
        d.getTopPositiveContributorsJson(),
        d.getTopNegativeContributorsJson()
    );
  }
}
