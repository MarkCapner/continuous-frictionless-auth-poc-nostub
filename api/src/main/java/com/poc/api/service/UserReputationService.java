package com.poc.api.service;

import com.poc.api.persistence.DeviceProfile;
import com.poc.api.persistence.DeviceProfileRepository;
import com.poc.api.persistence.DecisionLogRepository;
import com.poc.api.persistence.DecisionLogRow;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * EPIC 6: User-level intelligence.
 *
 * Computes a coarse-grained reputation for a given user based on:
 *  - device diversity (how many profiles, TLS fingerprints, countries)
 *  - recent decision log confidence and volume
 *  - account sharing heuristics (many TLS fingerprints / countries for one user)
 *
 * This is deliberately heuristic and explainable rather than ML-based.
 */
@Service
public class UserReputationService {

  private final DeviceProfileRepository deviceProfileRepository;
  private final DecisionLogRepository decisionLogRepository;
  private final AccountSharingHeuristics accountSharingHeuristics;

  public UserReputationService(DeviceProfileRepository deviceProfileRepository,
                               DecisionLogRepository decisionLogRepository,
                               AccountSharingHeuristics accountSharingHeuristics) {
    this.deviceProfileRepository = deviceProfileRepository;
    this.decisionLogRepository = decisionLogRepository;
    this.accountSharingHeuristics = accountSharingHeuristics;
  }

  public record Reputation(
      double trustScore,           // 0..1
      double accountSharingRisk,   // 0..1
      int deviceCount,
      int tlsFingerprintCount,
      int countryCount,
      double avgConfidenceRecent,
      long sessionsLast30d
  ) {}

  public Reputation evaluate(String userId) {
    if (userId == null || userId.isBlank()) {
      return new Reputation(0.5, 0.5, 0, 0, 0, 0.5, 0);
    }

    List<DeviceProfile> profiles = deviceProfileRepository.findByUser(userId);
    int deviceCount = profiles.size();

    // Basic cross-device correlation: how many distinct TLS fingerprints & countries?
    java.util.Set<String> tlsFps = new java.util.HashSet<>();
    java.util.Set<String> countries = new java.util.HashSet<>();
    for (DeviceProfile p : profiles) {
      if (p.tlsFp != null && !p.tlsFp.isBlank()) {
        tlsFps.add(p.tlsFp);
      }
      if (p.lastCountry != null && !p.lastCountry.isBlank()) {
        countries.add(p.lastCountry);
      }
    }
    int tlsCount = tlsFps.size();
    int countryCount = countries.size();

    AccountSharingHeuristics.Result sharing = accountSharingHeuristics.evaluate(userId);

    // Recent decisions for confidence & volume
    List<DecisionLogRow> recent = decisionLogRepository.findRecentByUser(userId, 100);
    double avgConfidence = 0.5;
    long sessions30d = 0;
    if (!recent.isEmpty()) {
      double sum = 0.0;
      for (DecisionLogRow row : recent) {
        sum += row.confidence;
      }
      avgConfidence = sum / recent.size();

      OffsetDateTime cutoff = OffsetDateTime.now(ZoneOffset.UTC).minus(30, ChronoUnit.DAYS);
      for (DecisionLogRow row : recent) {
        if (row.createdAt != null && !row.createdAt.isBefore(cutoff)) {
          sessions30d++;
        }
      }
    }

    // Compute account sharing risk in [0,1]
    double sharingRisk = 0.0;
    if (sharing.suspicious()) {
      sharingRisk = 0.8;
    } else {
      // Slightly higher risk as the number of devices / countries grows
      sharingRisk = Math.min(0.7,
          0.2 + 0.05 * Math.max(0, deviceCount - 1) + 0.1 * Math.max(0, countryCount - 1));
    }

    // Trust score starts from recent confidence and is penalised by sharingRisk.
    double baseTrust = clamp(avgConfidence, 0.0, 1.0);
    double penalty = 0.5 * sharingRisk;
    // Reward a modest amount of history (more sessions → a bit more trust up to a point)
    double historyBoost = Math.min(0.15, sessions30d * 0.01);
    double trust = baseTrust * (1.0 - penalty) + historyBoost;
    trust = clamp(trust, 0.0, 1.0);

    return new Reputation(trust, sharingRisk, deviceCount, tlsCount, countryCount, avgConfidence, sessions30d);
  }

  private static double clamp(double v, double min, double max) {
    if (v < min) return min;
    if (v > max) return max;
    return v;
  }
}
