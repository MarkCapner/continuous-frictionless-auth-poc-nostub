package com.poc.api.service;

import com.poc.api.model.Telemetry;
import com.poc.api.persistence.DeviceProfile;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class FeatureBuilder {

  public record Features(
      double deviceScore,
      double behaviorScore,
      double tlsScore,
      double contextScore
  ) {}

  public Features build(DeviceProfile deviceProfile,
                        double behaviorSimilarity,
                        String tlsFp,
                        Telemetry telemetry,
                        Map<String, Object> context) {

    Telemetry.Device d = telemetry.device();

    double deviceSim = 0.5;
    if (deviceProfile != null && d != null && d.screen() != null) {
      var screen = d.screen();
      double wDelta = Math.abs(deviceProfile.screenW - screen.w());
      double hDelta = Math.abs(deviceProfile.screenH - screen.h());
      double prDelta = Math.abs(deviceProfile.pixelRatio - screen.pixel_ratio());

      double wSim = Math.exp(-wDelta / 200.0);
      double hSim = Math.exp(-hDelta / 200.0);
      double prSim = Math.exp(-prDelta / 1.0);

      double tzDelta = Math.abs(deviceProfile.tzOffset - d.tz_offset());
      double tzSim = Math.exp(-tzDelta / 60.0);

      deviceSim = (wSim + hSim + prSim + tzSim) / 4.0;
    }

    double deviceScore = deviceSim;
    double behaviorScore = behaviorSimilarity;

    double tlsScore = (tlsFp != null && !tlsFp.isBlank()) ? 0.9 : 0.5;

    double contextScore = 0.5;
    if (context != null) {
      Object hourObj = context.get("hour");
      Integer hour = null;
      if (hourObj instanceof Number hourNum) {
        hour = hourNum.intValue();
      }
      boolean weekend = false;
      Object weekendObj = context.get("weekend");
      if (weekendObj instanceof Boolean b) {
        weekend = b;
      }
      if (hour != null) {
        // reward typical working hours on non-weekend days
        if (!weekend && hour >= 8 && hour <= 20) {
          contextScore = 0.7;
        } else {
          contextScore = 0.5;
        }
      }
    }

    return new Features(deviceScore, behaviorScore, tlsScore, contextScore);
  }
}
