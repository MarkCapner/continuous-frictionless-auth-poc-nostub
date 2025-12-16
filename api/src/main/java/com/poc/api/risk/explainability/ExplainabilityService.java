package com.poc.api.risk.explainability;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poc.api.risk.model.FeatureContribution;
import org.springframework.stereotype.Service;

@Service
public class ExplainabilityService {
  private final SessionExplainabilityRepository repo;
  private final ObjectMapper om = new ObjectMapper();

  public ExplainabilityService(SessionExplainabilityRepository repo) {
    this.repo = repo;
  }

  public void store(String sessionId, FeatureContribution[] contributions) {
    try {
      repo.save(new SessionExplainability(
          sessionId,
          om.writeValueAsString(contributions)
      ));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
