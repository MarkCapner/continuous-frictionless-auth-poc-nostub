package com.poc.api.service;

import java.time.Duration;
import java.time.OffsetDateTime;

/**
 * EPIC 9.1.5: Deterministic TLS family confidence/stability scoring.
 *
 * This is a PoC-friendly heuristic intended to be:
 *  - simple,
 *  - deterministic,
 *  - bounded to [0,1],
 *  - driven by observation volume, family variant dispersion and recency.
 */
public final class TlsFamilyScoring {

  private TlsFamilyScoring() {}

  public static Scores compute(long observationCount, int variantCount, OffsetDateTime lastSeen, OffsetDateTime now) {
    long obs = Math.max(1L, observationCount);
    int variants = Math.max(1, variantCount);

    // Recency decay: families not seen recently are less trusted.
    double recencyFactor = 1.0;
    if (lastSeen != null && now != null) {
      long days = Math.max(0L, Duration.between(lastSeen, now).toDays());
      recencyFactor = Math.exp(-days / 30.0); // 30-day half-life-ish
    }

    // Observation saturation: grows quickly early then plateaus.
    double obsFactor = 1.0 - Math.exp(-obs / 25.0);

    // Variant penalty: more variants = more heterogeneity.
    double variantPenalty = 1.0 / (1.0 + 0.20 * Math.max(0, variants - 1));

    double confidence = clamp01(obsFactor * variantPenalty * recencyFactor);

    // Stability: stronger penalty on variants and requires more observations.
    double stabilityObsFactor = 1.0 - Math.exp(-obs / 60.0);
    double stabilityVariantPenalty = 1.0 / (1.0 + 0.35 * Math.max(0, variants - 1));
    double stability = clamp01(stabilityObsFactor * stabilityVariantPenalty * recencyFactor);

    return new Scores(confidence, stability);
  }

  private static double clamp01(double v) {
    if (Double.isNaN(v) || Double.isInfinite(v)) return 0.0;
    if (v < 0.0) return 0.0;
    if (v > 1.0) return 1.0;
    return v;
  }

  public record Scores(double confidence, double stability) {}
}
