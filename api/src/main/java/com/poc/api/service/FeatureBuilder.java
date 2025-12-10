package com.poc.api.service;

import com.poc.api.model.Telemetry;
import com.poc.api.persistence.DeviceProfile;
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
                        String tlsFp, Telemetry telemetry) {

    // Device similarity based on deltas to stored profile
    Telemetry.Device d = telemetry.device();
    double deviceSim = 0.5;

    if (deviceProfile != null && d != null && d.screen() != null) {
      double screenWDelta = Math.abs(d.screen().w() - deviceProfile.screenW);
      double screenHDelta = Math.abs(d.screen().h() - deviceProfile.screenH);
      double pixelRatioDelta = Math.abs(d.screen().pixel_ratio() - deviceProfile.pixelRatio);
      double tzDelta = Math.abs((double) d.tz_offset() - deviceProfile.tzOffset);

      double wSim = Math.exp(-screenWDelta / 200.0);
      double hSim = Math.exp(-screenHDelta / 200.0);
      double prSim = Math.exp(-pixelRatioDelta / 0.5);
      double tzSim = Math.exp(-tzDelta / 60.0);

      deviceSim = 0.25 * (wSim + hSim + prSim + tzSim);
    }

    double deviceScore = deviceSim;
    double behaviorScore = behaviorSimilarity;

    double tlsScore = (tlsFp != null && !tlsFp.isBlank()) ? 0.9 : 0.5;

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
