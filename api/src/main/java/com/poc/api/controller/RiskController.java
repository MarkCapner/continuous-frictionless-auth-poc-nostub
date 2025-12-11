package com.poc.api.controller;

import com.poc.api.model.DecisionResponse;
import com.poc.api.model.Telemetry;
import com.poc.api.persistence.DecisionLogRepository;
import com.poc.api.persistence.DeviceProfileRepository;
import com.poc.api.persistence.TlsFingerprintStatsRow;
import com.poc.api.persistence.TlsFingerprintDeviceRow;
import com.poc.api.persistence.DecisionLogRow;
import com.poc.api.persistence.UserSummaryRow;
import com.poc.api.persistence.BehaviorBaselineRow;
import com.poc.api.persistence.BehaviorStatRepository;
import com.poc.api.service.RiskService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class RiskController {

  private final RiskService riskService;
  private final DecisionLogRepository decisionLogRepository;
  private final DeviceProfileRepository deviceProfileRepository;
  private final BehaviorStatRepository behaviorStatRepository;

  public RiskController(RiskService riskService,
      DecisionLogRepository decisionLogRepository,
      DeviceProfileRepository deviceProfileRepository,
      BehaviorStatRepository behaviorStatRepository) {
    this.riskService = riskService;
    this.decisionLogRepository = decisionLogRepository;
    this.deviceProfileRepository = deviceProfileRepository;
    this.behaviorStatRepository = behaviorStatRepository;
  }

  @PostMapping("/auth/profile-check")
  public ResponseEntity<DecisionResponse> profileCheck(
      @RequestHeader(value = "X-TLS-FP", required = false) String tlsFp,
      @RequestHeader(value = "X-TLS-Meta", required = false) String tlsMeta,
      @RequestHeader(value = "X-Request-Id", required = false) String requestId,
      @Valid @RequestBody Telemetry telemetry,
      HttpServletRequest request
  ) {
    String ip = request.getRemoteAddr();
    DecisionResponse resp = riskService.score(tlsFp, tlsMeta, telemetry, ip, requestId);
    return ResponseEntity.ok(resp);
  }



  @GetMapping("/showcase/tls-fp")
  public ResponseEntity<TlsFingerprintStatsRow> tlsFingerprintStats(
      @RequestParam("fp") String tlsFp
  ) {
    return deviceProfileRepository.findTlsStats(tlsFp)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.notFound().build());
  }


  @GetMapping("/showcase/tls-fp/devices")
  public ResponseEntity<java.util.List<TlsFingerprintDeviceRow>> tlsFingerprintDevices(
      @RequestParam("fp") String tlsFp
  ) {
    var rows = deviceProfileRepository.findDevicesByTlsFp(tlsFp);
    return ResponseEntity.ok(rows);
  }

  @GetMapping("/admin/tls-fps")
  public ResponseEntity<java.util.List<TlsFingerprintStatsRow>> allTlsFingerprints(
      @RequestParam(name = "limit", defaultValue = "100") int limit
  ) {
    var rows = deviceProfileRepository.findAllTlsStats(limit);
    return ResponseEntity.ok(rows);
  }

  @GetMapping("/showcase/users")
  public ResponseEntity<java.util.List<UserSummaryRow>> users(
      @RequestParam(name = "limit", defaultValue = "20") int limit
  ) {
    var rows = decisionLogRepository.findUserSummaries(limit);
    return ResponseEntity.ok(rows);
  }


  @GetMapping("/admin/behavior/baselines")
  public ResponseEntity<java.util.List<BehaviorBaselineRow>> behaviorBaselines(
      @RequestParam(name = "limit", defaultValue = "200") int limit
  ) {
    var stats = behaviorStatRepository.findRecent(limit);
    java.util.List<BehaviorBaselineRow> rows = stats.stream().map(s -> {
      BehaviorBaselineRow row = new BehaviorBaselineRow();
      row.userId = s.userId;
      row.feature = s.feature;
      row.mean = s.mean;
      row.variance = s.variance;
      row.stdDev = Math.sqrt(s.variance);
      row.decay = s.decay;
      row.updatedAt = s.updatedAt;
      return row;
    }).toList();
    return ResponseEntity.ok(rows);
  }


  @GetMapping("/showcase/sessions")
  public ResponseEntity<List<DecisionLogRow>> sessions(
      @RequestParam("user_hint") String userHint,
      @RequestParam(name = "limit", defaultValue = "20") int limit
  ) {
    var rows = decisionLogRepository.findRecentByUser(userHint, limit);
    return ResponseEntity.ok(rows);
  }
}