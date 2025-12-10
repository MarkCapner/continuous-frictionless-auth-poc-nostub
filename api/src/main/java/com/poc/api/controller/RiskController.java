package com.poc.api.controller;

import com.poc.api.model.DecisionResponse;
import com.poc.api.model.Telemetry;
import com.poc.api.persistence.DecisionLogRepository;
import com.poc.api.persistence.DecisionLogRow;
import com.poc.api.service.RiskService;
import com.poc.api.web.TelemetryValidator;
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
  private final TelemetryValidator telemetryValidator;

  public RiskController(RiskService riskService,
                        DecisionLogRepository decisionLogRepository,
                        TelemetryValidator telemetryValidator) {
    this.riskService = riskService;
    this.decisionLogRepository = decisionLogRepository;
    this.telemetryValidator = telemetryValidator;
  }

  @PostMapping("/auth/profile-check")
  public ResponseEntity<DecisionResponse> profileCheck(
      @RequestHeader(name = "X-TLS-FP", required = false) String tlsFp,
      @RequestHeader(name = "X-Request-Id", required = false) String requestId,
      @Valid @RequestBody Telemetry telemetry,
      HttpServletRequest request
  ) {
    telemetryValidator.validate(telemetry);
    String ip = request.getRemoteAddr();
    DecisionResponse resp = riskService.score(tlsFp, telemetry, ip, requestId);
    return ResponseEntity.ok(resp);
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
