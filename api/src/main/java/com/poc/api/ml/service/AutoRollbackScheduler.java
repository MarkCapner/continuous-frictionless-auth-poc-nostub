package com.poc.api.ml.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poc.api.ml.persistence.ModelChangeEventRepository;
import com.poc.api.ml.persistence.ModelScorecardRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AutoRollbackScheduler {

  private final ModelScorecardRepository scorecards;
  private final ModelChangeEventRepository changes;
  private final ModelRolloutService rollout;
  private final ObjectMapper om = new ObjectMapper();

  public AutoRollbackScheduler(ModelScorecardRepository scorecards,
                               ModelChangeEventRepository changes,
                               ModelRolloutService rollout) {
    this.scorecards = scorecards;
    this.changes = changes;
    this.rollout = rollout;
  }

  @Scheduled(fixedDelayString = "${poc.rollback.scan.delay-ms:30000}")
  public void scan() {
    for (var sc : scorecards.list("GLOBAL", "*", 25)) {
      if (!"REGRESSION".equalsIgnoreCase(sc.status())) continue;
      if (!"activation".equalsIgnoreCase(sc.triggerType())) continue;
      if (changes.hasRollbackForScorecard(sc.id())) continue;

      try {
        String evidence = om.writeValueAsString(Map.of(
            "scorecardId", sc.id(),
            "status", sc.status(),
            "delta", sc.deltaMetricsJson()
        ));
        rollout.rollback("system", sc.scopeType(), sc.scopeKey(), null, "auto:regression", evidence);
      } catch (Exception ignore) {}
    }
  }
}
