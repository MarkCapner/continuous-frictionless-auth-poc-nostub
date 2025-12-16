package com.poc.api.risk.explainability;

import java.util.*;
import com.poc.api.risk.model.FeatureContribution;
import org.springframework.stereotype.Service;

@Service
public class CrossSessionExplainabilityService {

  private final Map<String, List<Double>> history = new HashMap<>();

  public void update(FeatureContribution[] contributions) {
    for (FeatureContribution c : contributions) {
      history.computeIfAbsent(c.key(), k -> new ArrayList<>())
             .add(c.contribution());
    }
  }

  public Map<String, Double> deltas(FeatureContribution[] current) {
    Map<String, Double> out = new HashMap<>();
    for (FeatureContribution c : current) {
      var hist = history.get(c.key());
      if (hist == null) continue;
      double avg = hist.stream().mapToDouble(d -> d).average().orElse(0.0);
      out.put(c.key(), c.contribution() - avg);
    }
    return out;
  }
}
