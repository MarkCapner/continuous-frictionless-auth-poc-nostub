package com.poc.api.service.drift;

import com.poc.api.model.DriftSummary;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class DriftRiskAdjuster {

  public record Adjustment(double adjustedConfidence, String enforcement, boolean stepUpRecommended, Map<String,Object> explain) {}

  public Adjustment adjust(double baseConfidence, DriftSummary drift) {
    double max = drift == null ? 0.0 : drift.maxDrift();
    double conf = clamp01(baseConfidence - (max * 0.30));

    String enforcement = "NONE";
    boolean stepUp = false;

    if (max >= 0.85) {
      enforcement = "DENY";
    } else if (max >= 0.60) {
      enforcement = "STEP_UP";
      stepUp = true;
    } else if (max >= 0.40) {
      stepUp = true;
    }

    Map<String,Object> explain = new LinkedHashMap<>();
    explain.put("max_drift", max);
    explain.put("confidence_penalty", baseConfidence - conf);
    explain.put("device_drift", drift == null ? 0 : drift.deviceDrift());
    explain.put("behavior_drift", drift == null ? 0 : drift.behaviorDrift());
    explain.put("tls_drift", drift == null ? 0 : drift.tlsDrift());
    explain.put("feature_drift", drift == null ? 0 : drift.featureDrift());
    explain.put("model_instability", drift == null ? 0 : drift.modelInstability());
    explain.put("enforcement", enforcement);
    explain.put("step_up_recommended", stepUp);

    return new Adjustment(conf, enforcement, stepUp, explain);
  }

  private double clamp01(double v) {
    if (v < 0) return 0;
    if (v > 1) return 1;
    return v;
  }
}
