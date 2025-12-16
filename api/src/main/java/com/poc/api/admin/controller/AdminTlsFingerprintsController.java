package com.poc.api.admin.controller;

import com.poc.api.telemetry.persistence.DeviceProfileRepository;
import com.poc.api.telemetry.persistence.TlsFingerprintStatsRow;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/api/admin","/api/v1/admin"})
public class AdminTlsFingerprintsController {

  private final DeviceProfileRepository deviceProfileRepository;

  public AdminTlsFingerprintsController(DeviceProfileRepository deviceProfileRepository) {
    this.deviceProfileRepository = deviceProfileRepository;
  }

  @GetMapping("/tls-fps")
  public ResponseEntity<List<TlsFingerprintStatsRow>> allTlsFingerprints(
      @RequestParam(name = "limit", defaultValue = "100") int limit
  ) {
    return ResponseEntity.ok(deviceProfileRepository.findAllTlsStats(limit));
  }
}
