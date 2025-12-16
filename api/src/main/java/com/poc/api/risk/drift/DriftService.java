package com.poc.api.risk.drift;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poc.api.risk.drift.dto.DriftSummary;
import com.poc.api.telemetry.dto.Telemetry;
import com.poc.api.risk.drift.persistence.DriftRepository;
import com.poc.api.telemetry.service.BehaviorStatsService;
import com.poc.api.showcase.service.TlsFamilyService;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

@Service
public class DriftService {

  private final DriftRepository repo;
  private final BehaviorStatsService behaviorStats;
  private final TlsFamilyService tlsFamilyService;
  private final ObjectMapper om;

  public DriftService(DriftRepository repo, BehaviorStatsService behaviorStats, TlsFamilyService tlsFamilyService, ObjectMapper om) {
    this.repo = repo;
    this.behaviorStats = behaviorStats;
    this.tlsFamilyService = tlsFamilyService;
    this.om = om;
  }

  public DriftSummary computeAndPersist(String userId, String requestId, Telemetry telemetry, double confidence,
                                        String tlsFp, String tlsMeta, String modelVersion) {
    if (userId == null || userId.isBlank()) userId = "anonymous";
    if (requestId == null || requestId.isBlank()) requestId = "req-" + System.currentTimeMillis();

    DriftRepository.DriftBaselineRow b = repo.getBaseline(userId);
    RunningStat confStat = b == null ? new RunningStat(0,0,0) : new RunningStat(b.confCount(), b.confMean(), b.confM2());

    String deviceSig = computeDeviceSig(telemetry != null ? telemetry.device() : null);

    String tlsFamily = null;
    if (tlsFp != null) {
      try {
        var obs = tlsFamilyService.observe(userId, tlsFp, tlsMeta);
        tlsFamily = obs.familyId();
      } catch (Exception ignore) {
        tlsFamily = null;
      }
    }

    double deviceDrift = 0.0;
    if (b != null && b.lastDeviceSig() != null && deviceSig != null) {
      deviceDrift = b.lastDeviceSig().equals(deviceSig) ? 0.0 : 1.0;
    } else if (b != null && b.lastDeviceSig() != null && deviceSig == null) {
      deviceDrift = 0.5;
    }

    double behaviorDrift = 0.0;
    if (telemetry != null && telemetry.behavior() != null) {
      try {
        var sim = behaviorStats.updateAndComputeSimilarity(userId, telemetry.behavior());
        behaviorDrift = clamp01(1.0 - sim.score());
      } catch (Exception ignore) {
        behaviorDrift = 0.25;
      }
    }

    double tlsDrift = 0.0;
    if (b != null && b.lastTlsFamily() != null && tlsFamily != null) {
      tlsDrift = b.lastTlsFamily().equals(tlsFamily) ? 0.0 : 1.0;
    } else if (b != null && b.lastTlsFamily() != null && tlsFamily == null) {
      tlsDrift = 0.5;
    }

    double featureDrift = 0.0;
    if (confStat.n() >= 20) {
      double z = Math.abs(confStat.zscore(confidence));
      featureDrift = clamp01(z / 3.0);
    }

    double modelInstability = 0.0;
    if (b != null && b.lastModelVersion() != null && modelVersion != null) {
      modelInstability = b.lastModelVersion().equals(modelVersion) ? 0.0 : 0.35;
    }

    double max = Math.max(deviceDrift, Math.max(behaviorDrift, Math.max(tlsDrift, Math.max(featureDrift, modelInstability))));

    List<String> warnings = new ArrayList<>();
    if (deviceDrift >= 0.9) warnings.add("DEVICE_CHANGED");
    if (tlsDrift >= 0.9) warnings.add("TLS_FAMILY_CHANGED");
    if (behaviorDrift >= 0.7) warnings.add("BEHAVIOR_DRIFT_HIGH");
    if (featureDrift >= 0.7) warnings.add("CONFIDENCE_SHIFT");
    if (modelInstability >= 0.3) warnings.add("MODEL_VERSION_CHANGED");

    try {
      repo.insertEvent(userId, requestId, deviceDrift, behaviorDrift, tlsDrift, featureDrift, modelInstability, max,
          om.writeValueAsString(warnings));
    } catch (Exception ignore) {}

    confStat.push(confidence);
    repo.upsertBaseline(new DriftRepository.DriftBaselineRow(
        userId,
        OffsetDateTime.now(),
        deviceSig,
        tlsFamily,
        modelVersion,
        confStat.n(),
        confStat.mean(),
        confStat.m2()
    ));

    return new DriftSummary(deviceDrift, behaviorDrift, tlsDrift, featureDrift, modelInstability, max, warnings, tlsFamily, deviceSig);
  }

  private String computeDeviceSig(Telemetry.Device d) {
    if (d == null) return null;
    String raw = String.join("|",
        safe(d.ua()),
        safe(d.platform()),
        safe(String.valueOf(d.tz_offset())),
        d.screen() == null ? "" : (safe(String.valueOf(d.screen().w())) + "x" + safe(String.valueOf(d.screen().h())) + "@" + safe(String.valueOf(d.screen().pixel_ratio()))),
        safe(d.canvas_hash()),
        safe(d.webgl_hash())
    );
    return sha256Hex(raw);
  }

  private String sha256Hex(String s) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] out = md.digest(s.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(out);
    } catch (Exception e) {
      return Integer.toHexString(s.hashCode());
    }
  }

  private String safe(String s) { return s == null ? "" : s; }

  private double clamp01(double v) {
    if (v < 0) return 0;
    if (v > 1) return 1;
    return v;
  }
}
