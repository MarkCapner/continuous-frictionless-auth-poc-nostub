package com.poc.api.risk.explainability;

import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.poc.api.risk.model.FeatureContribution;
import org.springframework.stereotype.Service;

@Service
public class ExplainabilityNarrativeService {

  private final SessionExplainabilityRepository repo;
  private final ObjectMapper om = new ObjectMapper();

  public ExplainabilityNarrativeService(SessionExplainabilityRepository repo) {
    this.repo = repo;
  }

  public String narrative(String sessionId) {
    try {
      var s = repo.findBySessionId(sessionId).orElseThrow();
      FeatureContribution[] arr =
          om.readValue(s.getContributionsJson(), FeatureContribution[].class);

      List<String> positives = new ArrayList<>();
      for (FeatureContribution c : arr) {
        if (c.contribution() > 0) positives.add(c.key());
      }

      return positives.isEmpty()
          ? "We took extra precautions for this login."
          : "We trusted this login because " + String.join(", ", positives) + ".";

    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
