package com.poc.api.risk.service;

import com.poc.api.telemetry.dto.Telemetry;
import com.poc.api.telemetry.persistence.DeviceProfile;
import org.springframework.stereotype.Service;

@Service
public class FeatureBuilder {

  public record Features(
      double deviceScore,
      double behaviorScore,
      double tlsScore,
      double contextScore
  ) {}

  public Features build(DeviceProfile deviceProfile, double behaviorSimilarity,
                        String tlsFp, Telemetry telemetry, Double tlsScoreOverride) {

    // Device similarity based on deltas to stored profile + fingerprint hashes
    Telemetry.Device d = telemetry.device();
    double deviceSim = 0.5;

    if (deviceProfile != null && d != null && d.screen() != null) {
      double screenWDelta = Math.abs(d.screen().w() - deviceProfile.screenW);
      double screenHDelta = Math.abs(d.screen().h() - deviceProfile.screenH);
      double pixelRatioDelta = Math.abs(d.screen().pixel_ratio() - deviceProfile.pixelRatio);
      double tzDeltaMinutes = Math.abs((double) d.tz_offset() - deviceProfile.tzOffset);

      double wSim = Math.exp(-screenWDelta / 200.0);
      double hSim = Math.exp(-screenHDelta / 200.0);
      double prSim = Math.exp(-pixelRatioDelta / 0.5);
      double tzSim = Math.exp(-tzDeltaMinutes / 60.0); // minutes → hours-ish

      // Canvas & WebGL hashes: strong signals – mismatch is a big drop in similarity
      double canvasSim = 1.0;
      if (d.canvas_hash() != null && deviceProfile.canvasHash != null) {
        canvasSim = d.canvas_hash().equals(deviceProfile.canvasHash) ? 1.0 : 0.0;
      }
      double webglSim = 1.0;
      if (d.webgl_hash() != null && deviceProfile.webglHash != null) {
        webglSim = d.webgl_hash().equals(deviceProfile.webglHash) ? 1.0 : 0.0;
      }

      // Aggregate into a single consistency score
      deviceSim = (wSim + hSim + prSim + tzSim + canvasSim + webglSim) / 6.0;
    }

    double deviceScore = deviceSim;
    double behaviorScore = behaviorSimilarity;

    double tlsScore;
    if (tlsScoreOverride != null) {
      tlsScore = tlsScoreOverride;
    } else {
      tlsScore = (tlsFp != null && !tlsFp.isBlank()) ? 0.9 : 0.5;
    }

    double contextScore = 0.5;
    if (telemetry.context() != null) {
      Object hourObj = telemetry.context().get("hour");
      if (hourObj instanceof Number hourNum) {
        int hour = hourNum.intValue();
        contextScore = (hour >= 8 && hour <= 20) ? 0.7 : 0.5;
      }
    }

    return new Features(deviceScore, behaviorScore, tlsScore, contextScore);
  }
}