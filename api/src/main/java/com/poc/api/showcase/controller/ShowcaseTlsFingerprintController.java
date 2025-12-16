package com.poc.api.showcase.controller;

import com.poc.api.telemetry.persistence.DeviceProfileRepository;
import com.poc.api.telemetry.persistence.TlsFingerprintDeviceRow;
import com.poc.api.telemetry.persistence.TlsFingerprintStatsRow;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping({"/api/showcase","/api/v1/showcase"})
public class ShowcaseTlsFingerprintController {

  private final DeviceProfileRepository deviceProfileRepository;

  public ShowcaseTlsFingerprintController(DeviceProfileRepository deviceProfileRepository) {
    this.deviceProfileRepository = deviceProfileRepository;
  }

  @GetMapping("/tls-fp")
  public ResponseEntity<TlsFingerprintStatsRow> tlsFingerprintStats(@RequestParam("fp") String tlsFp) {
    return deviceProfileRepository.findTlsStats(tlsFp)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  @GetMapping("/tls-fp/devices")
  public ResponseEntity<List<TlsFingerprintDeviceRow>> tlsFingerprintDevices(@RequestParam("fp") String tlsFp) {
    return ResponseEntity.ok(deviceProfileRepository.findDevicesByTlsFp(tlsFp));
  }

  public record TlsFpOverview(
      String tlsFp,
      long profiles,
      long users,
      OffsetDateTime firstSeen,
      OffsetDateTime lastSeen,
      List<TlsFingerprintDeviceRow> devices
  ) {}

  @GetMapping("/tls-fp/overview")
  public ResponseEntity<TlsFpOverview> tlsFpOverview(@RequestParam("tls_fp") String tlsFp) {
    var statsOpt = deviceProfileRepository.findTlsStats(tlsFp);
    var devices = deviceProfileRepository.findDevicesByTlsFp(tlsFp);
    if (statsOpt.isEmpty() && devices.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    TlsFingerprintStatsRow stats = statsOpt.orElseGet(() -> {
      TlsFingerprintStatsRow row = new TlsFingerprintStatsRow();
      row.tlsFp = tlsFp;
      row.profiles = devices.size();
      row.users = devices.stream().map(d -> d.userId).distinct().count();
      row.firstSeen = devices.stream()
          .map(d -> d.firstSeen)
          .filter(java.util.Objects::nonNull)
          .min(OffsetDateTime::compareTo)
          .orElse(null);
      row.lastSeen = devices.stream()
          .map(d -> d.lastSeen)
          .filter(java.util.Objects::nonNull)
          .max(OffsetDateTime::compareTo)
          .orElse(null);
      return row;
    });

    return ResponseEntity.ok(new TlsFpOverview(
        stats.tlsFp,
        stats.profiles,
        stats.users,
        stats.firstSeen,
        stats.lastSeen,
        devices
    ));
  }
}
