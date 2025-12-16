package com.poc.api.risk.explainability.admin;

import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.poc.api.risk.explainability.*;
import com.poc.api.risk.model.FeatureContribution;
import org.springframework.stereotype.Service;

@Service
public class ExplainabilityAdminAnalyticsService {

  private final SessionExplainabilityRepository repo;
  private final ObjectMapper om = new ObjectMapper();

  public ExplainabilityAdminAnalyticsService(SessionExplainabilityRepository repo) {
    this.repo = repo;
  }

  public Map<String, Long> topFeatures() {
    Map<String, Long> counts = new HashMap<>();

    for (SessionExplainability s : repo.findAll()) {
      try {
        FeatureContribution[] arr =
            om.readValue(s.getContributionsJson(), FeatureContribution[].class);
        for (FeatureContribution c : arr) {
          counts.merge(c.key(), 1L, Long::sum);
        }
      } catch (Exception ignore) {}
    }
    return counts;
  }
}
