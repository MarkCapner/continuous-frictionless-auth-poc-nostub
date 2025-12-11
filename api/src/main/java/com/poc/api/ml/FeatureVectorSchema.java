package com.poc.api.ml;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Defines the fixed feature-vector layout for the ML model.
 *
 * EPIC 5 expanded schema:
 *  - device_score
 *  - behavior_score
 *  - tls_score
 *  - context_score
 *  - behavior_z_key_interval_mean
 *  - behavior_z_key_interval_std
 *  - behavior_z_scroll_rate
 *  - device_seen_count_log
 *  - tls_fp_seen_count
 *  - ml_anomaly_score     (self-referential feature to help the classifier learn patterns)
 *
 * The additional dimensions are derived from the enriched breakdown map that
 * RiskService already persists into session_feature.feature_vector.
 */
public final class FeatureVectorSchema {

  public static final String DEVICE_SCORE = "device_score";
  public static final String BEHAVIOR_SCORE = "behavior_score";
  public static final String TLS_SCORE = "tls_score";
  public static final String CONTEXT_SCORE = "context_score";

  // Selected behaviour z-scores
  public static final String BEHAVIOR_Z_KEY_INTERVAL_MEAN = "behavior_z_key_interval_mean";
  public static final String BEHAVIOR_Z_KEY_INTERVAL_STD = "behavior_z_key_interval_std";
  public static final String BEHAVIOR_Z_SCROLL_RATE = "behavior_z_scroll_rate";

  // Device / TLS rarity & drift style features
  public static final String DEVICE_SEEN_COUNT_LOG = "device_seen_count_log";
  public static final String TLS_FP_SEEN_COUNT = "tls_fp_seen_count";

  // ML-level anomaly score fed back as a feature
  public static final String ML_ANOMALY_SCORE = "ml_anomaly_score";

  private static final List<String> ORDER = Arrays.asList(
      DEVICE_SCORE,
      BEHAVIOR_SCORE,
      TLS_SCORE,
      CONTEXT_SCORE,
      BEHAVIOR_Z_KEY_INTERVAL_MEAN,
      BEHAVIOR_Z_KEY_INTERVAL_STD,
      BEHAVIOR_Z_SCROLL_RATE,
      DEVICE_SEEN_COUNT_LOG,
      TLS_FP_SEEN_COUNT,
      ML_ANOMALY_SCORE
  );

  private FeatureVectorSchema() {
  }

  public static int size() {
    return ORDER.size();
  }

  public static List<String> featureNames() {
    return ORDER;
  }

  /**
   * Builds a dense feature vector from a generic breakdown map (such as the one
   * we persist in session_feature.feature_vector). Missing keys default to 0.0.
   */
  public static double[] fromBreakdown(Map<String, Double> breakdown) {
    double[] v = new double[ORDER.size()];
    for (int i = 0; i < ORDER.size(); i++) {
      String name = ORDER.get(i);
      Double d = breakdown != null ? breakdown.get(name) : null;
      v[i] = d != null ? d : 0.0;
    }
    return v;
  }

  /**
   * Convenience helper from the four core scores and a few extras. Callers that
   * don't have all extended metrics available can still use this to construct a
   * vector; the remaining dimensions will be zeroed.
   */
  public static double[] fromScores(
      double deviceScore,
      double behaviorScore,
      double tlsScore,
      double contextScore
  ) {
    double[] v = new double[ORDER.size()];
    v[0] = deviceScore;
    v[1] = behaviorScore;
    v[2] = tlsScore;
    v[3] = contextScore;
    // The remaining slots are left at 0.0; they will be filled when we build
    // from the enriched breakdown during training.
    return v;
  }
}
