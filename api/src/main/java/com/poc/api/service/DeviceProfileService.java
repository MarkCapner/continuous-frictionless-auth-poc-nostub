package com.poc.api.service;

import com.poc.api.model.Telemetry;
import com.poc.api.persistence.DeviceProfile;
import com.poc.api.persistence.DeviceProfileRepository;
import org.springframework.stereotype.Service;

@Service
public class DeviceProfileService {

  private final DeviceProfileRepository repo;

  public DeviceProfileService(DeviceProfileRepository repo) {
    this.repo = repo;
  }

  public DeviceProfile upsert(String userId, String tlsFp, String country, Telemetry.Device d) {
    DeviceProfile p = new DeviceProfile();
    p.userId = userId != null ? userId : "anonymous";
    p.tlsFp = tlsFp != null ? tlsFp : "none";
    String ua = d.ua();
    if (ua != null) {
      String[] parts = ua.split(" ");
      p.uaFamily = parts[0];
      p.uaVersion = parts.length > 1 ? parts[1] : "unknown";
    } else {
      p.uaFamily = "unknown";
      p.uaVersion = "unknown";
    }

    Telemetry.Device.Screen s = d.screen();
    if (s != null) {
      p.screenW = s.w() != null ? s.w() : 0;
      p.screenH = s.h() != null ? s.h() : 0;
      p.pixelRatio = s.pixel_ratio() != null ? s.pixel_ratio() : 1.0;
    } else {
      p.screenW = 0;
      p.screenH = 0;
      p.pixelRatio = 1.0;
    }
    p.tzOffset = d.tz_offset() != null ? d.tz_offset().shortValue() : (short) 0;
    p.canvasHash = d.canvas_hash() != null ? d.canvas_hash() : "none";
    p.webglHash = d.webgl_hash();
    p.lastCountry = country;

    return repo.upsert(p);
  }
}
