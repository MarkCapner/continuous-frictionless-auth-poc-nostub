package com.poc.api.telemetry.service;

import com.poc.api.telemetry.dto.Telemetry;
import com.poc.api.telemetry.persistence.BehaviorStat;
import com.poc.api.telemetry.persistence.BehaviorStatRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class BehaviorStatsService {

  private final BehaviorStatRepository repo;

  public BehaviorStatsService(BehaviorStatRepository repo) {
    this.repo = repo;
  }

  public record BehaviorSimilarityResult(double score, Map<String, Double> zScores) {}

  /**
   * Updates per-user behavioural baselines (running mean/variance per feature)
   * and returns a similarity score in [0,1], where 1.0 means "looks like
   * the historical baseline" and values near 0 mean "very unusual".
   */
  public BehaviorSimilarityResult updateAndComputeSimilarity(String userId, Telemetry.Behavior behavior) {
    if (userId == null || userId.isBlank() || behavior == null) {
      // If we don't know who this is, fall back to neutral.
      return new BehaviorSimilarityResult(0.5, Map.of());
    }

    double scoreSum = 0.0;
    int count = 0;
    Map<String, Double> zScores = new HashMap<>();

    // Keystroke dynamics
    scoreSum += updateFeature(userId, "avg_key_interval_ms", behavior.avg_key_interval_ms(), 250.0, 0.9, zScores);
    count++;
    scoreSum += updateFeature(userId, "key_interval_std_ms", behavior.key_interval_std_ms(), 80.0, 0.9, zScores);
    count++;

    // Scroll cadence (events per second)
    scoreSum += updateFeature(userId, "scroll_events_per_sec", behavior.scroll_events_per_sec(), 1.0, 0.9, zScores);
    count++;

    // Pointer velocity profile
    scoreSum += updateFeature(userId, "pointer_avg_velocity", behavior.pointer_avg_velocity(), 0.3, 0.9, zScores);
    count++;
    scoreSum += updateFeature(userId, "pointer_max_velocity", behavior.pointer_max_velocity(), 1.0, 0.9, zScores);
    count++;

    // Overall mouse activity / distance
    scoreSum += updateFeature(userId, "mouse_distance", behavior.mouse_distance(), 800.0, 0.9, zScores);
    count++;

    double avgScore = count > 0 ? scoreSum / count : 0.5;
    if (avgScore < 0.0) avgScore = 0.0;
    if (avgScore > 1.0) avgScore = 1.0;

    return new BehaviorSimilarityResult(avgScore, zScores);
  }

  private double updateFeature(String userId, String feature, Double value,
                               double defaultVariance, double defaultDecay, Map<String, Double> zScores) {
    double v = value != null ? value : 0.0;

    BehaviorStat stat = repo.findByUserAndFeature(userId, feature)
        .orElseGet(() -> {
          BehaviorStat s = new BehaviorStat();
          s.userId = userId;
          s.feature = feature;
          s.mean = v;
          s.variance = defaultVariance;
          s.decay = defaultDecay;
          return s;
        });

    double decay = stat.decay;
    double meanNew = decay * stat.mean + (1.0 - decay) * v;
    double varNew = decay * stat.variance + (1.0 - decay) * Math.pow(v - meanNew, 2);
    stat.mean = meanNew;
    stat.variance = varNew <= 1e-6 ? 1e-6 : varNew; // avoid zero variance

    repo.save(stat);

    double std = Math.sqrt(stat.variance);
    double z = 0.0;
    double sim;
    if (std <= 0.0) {
      sim = 1.0;
    } else {
      z = (v - stat.mean) / std;
      sim = Math.exp(-0.5 * z * z);
    }
    zScores.put(feature, z);
    return sim;
  }
}
