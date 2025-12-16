package com.poc.api.risk.controller;

import com.poc.api.risk.dto.DecisionResponse;
import com.poc.api.risk.service.RiskService;
import com.poc.api.telemetry.dto.Telemetry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Risk decision endpoints.
 *
 * Backward compatible route kept at /api/auth/profile-check.
 * Canonical route is additionally exposed under /api/risk/profile-check.
 */
@RestController
@RequestMapping("/api")
public class RiskDecisionController {

  private final RiskService riskService;

  public RiskDecisionController(RiskService riskService) {
    this.riskService = riskService;
  }

  // Backward compatible
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

  // Preferred
  @PostMapping("/risk/profile-check")
  public ResponseEntity<DecisionResponse> riskProfileCheck(
      @RequestHeader(value = "X-TLS-FP", required = false) String tlsFp,
      @RequestHeader(value = "X-TLS-Meta", required = false) String tlsMeta,
      @RequestHeader(value = "X-Request-Id", required = false) String requestId,
      @Valid @RequestBody Telemetry telemetry,
      HttpServletRequest request
  ) {
    return profileCheck(tlsFp, tlsMeta, requestId, telemetry, request);
  }
}
