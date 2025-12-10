package com.poc.api.service;

import com.poc.api.model.Telemetry;
import com.poc.api.persistence.BehaviorStat;
import com.poc.api.persistence.BehaviorStatRepository;
import org.springframework.stereotype.Service;

@Service
public class BehaviorStatsService {

  private final BehaviorStatRepository repo;

  public BehaviorStatsService(BehaviorStatRepository repo) {
    this.repo = repo;
  }

  public record BehaviorSimilarityResult(double score) {}

  public BehaviorSimilarityResult updateAndComputeSimilarity(String userId, Telemetry.Behavior behavior) {
    if (userId == null || userId.isBlank() || behavior == null) {
      return new BehaviorSimilarityResult(0.5);
    }

    double keyInterval = behavior.avg_key_interval_ms() != null ? behavior.avg_key_interval_ms() : 0.0;
    BehaviorStat stat = repo.findByUserAndFeature(userId, "avg_key_interval_ms")
        .orElseGet(() -> {
          BehaviorStat s = new BehaviorStat();
          s.userId = userId;
          s.feature = "avg_key_interval_ms";
          s.mean = keyInterval;
          s.variance = 1.0;
          s.decay = 0.9;
          return s;
        });

    // Time-decayed update
    double decay = stat.decay;
    double meanNew = decay * stat.mean + (1 - decay) * keyInterval;
    double varNew = decay * stat.variance + (1 - decay) * Math.pow(keyInterval - meanNew, 2);
    stat.mean = meanNew;
    stat.variance = varNew <= 1e-6 ? 1e-6 : varNew; // avoid zero variance

    repo.save(stat);

    // z = (x - mean) / std
    double std = Math.sqrt(stat.variance);
    double z = std > 0 ? (keyInterval - stat.mean) / std : 0.0;
    double sim = Math.exp(-0.5 * z * z);
    return new BehaviorSimilarityResult(sim);
  }
}
