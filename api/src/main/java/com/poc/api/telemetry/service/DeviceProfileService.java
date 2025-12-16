package com.poc.api.telemetry.service;

import com.poc.api.telemetry.dto.Telemetry;
import com.poc.api.telemetry.persistence.DeviceProfile;
import com.poc.api.telemetry.persistence.DeviceProfileRepository;
import com.poc.api.identity.service.IdentityGraphService;
import org.springframework.stereotype.Service;

@Service
public class DeviceProfileService {

  private final DeviceProfileRepository repo;
  private final IdentityGraphService identityGraph;

  public DeviceProfileService(DeviceProfileRepository repo, IdentityGraphService identityGraph) {
    this.repo = repo;
    this.identityGraph = identityGraph;
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

    DeviceProfile saved = repo.upsert(p);

    // EPIC 10.2: best-effort identity graph observation
    if (identityGraph != null) {
      try { identityGraph.observeDeviceProfile(saved); } catch (Exception ignored) {}
    }

    return saved;
  }
}
