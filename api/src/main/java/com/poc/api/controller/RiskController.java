package com.poc.api.controller;

import com.poc.api.model.DecisionResponse;
import com.poc.api.model.Telemetry;
import com.poc.api.persistence.DecisionLogRepository;
import com.poc.api.persistence.DeviceProfile;
import com.poc.api.persistence.DeviceProfileRepository;
import com.poc.api.persistence.SessionFeatureRepository;
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
  private final SessionFeatureRepository sessionFeatureRepository;

  public RiskController(RiskService riskService,
      DecisionLogRepository decisionLogRepository,
      DeviceProfileRepository deviceProfileRepository,
      BehaviorStatRepository behaviorStatRepository,
      SessionFeatureRepository sessionFeatureRepository) {
    this.riskService = riskService;
    this.decisionLogRepository = decisionLogRepository;
    this.deviceProfileRepository = deviceProfileRepository;
    this.behaviorStatRepository = behaviorStatRepository;
    this.sessionFeatureRepository = sessionFeatureRepository;
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

  public record DeviceSummary(
      Long id,
      String userId,
      String tlsFp,
      String uaFamily,
      String uaVersion,
      int screenW,
      int screenH,
      double pixelRatio,
      short tzOffset,
      String canvasHash,
      String webglHash,
      java.time.OffsetDateTime firstSeen,
      java.time.OffsetDateTime lastSeen,
      long seenCount,
      String lastCountry
  ) {}

  public record DeviceDiffChange(
      String field,
      String kind,
      String leftValue,
      String rightValue
  ) {}

  public record DeviceDiffResponse(
      DeviceSummary left,
      DeviceSummary right,
      java.util.List<DeviceDiffChange> changes
  ) {}

  public record BehaviorHistoryItem(
      Long id,
      java.time.OffsetDateTime occurredAt,
      String tlsFp,
      String decision,
      double confidence,
      String behaviorJson,
      String featureVector,
      String label
  ) {}

  public record RiskTimelineItem(
      Long id,
      java.time.OffsetDateTime occurredAt,
      String decision,
      double confidence,
      double behaviorScore,
      double deviceScore,
      double contextScore,
      java.util.List<String> explanations
  ) {}

  public record TlsFpOverview(
      String tlsFp,
      long profiles,
      long users,
      java.time.OffsetDateTime firstSeen,
      java.time.OffsetDateTime lastSeen,
      java.util.List<TlsFingerprintDeviceRow> devices
  ) {}

  private DeviceSummary toDeviceSummary(DeviceProfile p) {
    return new DeviceSummary(
        p.id,
        p.userId,
        p.tlsFp,
        p.uaFamily,
        p.uaVersion,
        p.screenW,
        p.screenH,
        p.pixelRatio,
        p.tzOffset,
        p.canvasHash,
        p.webglHash,
        p.firstSeen,
        p.lastSeen,
        p.seenCount,
        p.lastCountry
    );
  }

  private java.util.List<DeviceDiffChange> computeDeviceDiff(DeviceProfile left, DeviceProfile right) {
    java.util.List<DeviceDiffChange> changes = new java.util.ArrayList<>();
    addDiff(changes, "uaFamily", left.uaFamily, right.uaFamily);
    addDiff(changes, "uaVersion", left.uaVersion, right.uaVersion);
    addDiff(changes, "screenW", String.valueOf(left.screenW), String.valueOf(right.screenW));
    addDiff(changes, "screenH", String.valueOf(left.screenH), String.valueOf(right.screenH));
    addDiff(changes, "pixelRatio", String.valueOf(left.pixelRatio), String.valueOf(right.pixelRatio));
    addDiff(changes, "tzOffset", String.valueOf(left.tzOffset), String.valueOf(right.tzOffset));
    addDiff(changes, "canvasHash", left.canvasHash, right.canvasHash);
    addDiff(changes, "webglHash", left.webglHash, right.webglHash);
    addDiff(changes, "lastCountry", left.lastCountry, right.lastCountry);
    return changes;
  }

  private void addDiff(java.util.List<DeviceDiffChange> changes, String field, String left, String right) {
    if (!java.util.Objects.equals(left, right)) {
      changes.add(new DeviceDiffChange(field, "CHANGED",
          left != null ? left : "", right != null ? right : ""));
    }
  }

  private RiskTimelineItem toRiskTimelineItem(DecisionLogRow r) {
    java.util.List<String> explanations = new java.util.ArrayList<>();

    if (r.deviceScore >= 0.8) {
      explanations.add("Trusted device (device score " + String.format("%.2f", r.deviceScore) + ").");
    } else if (r.deviceScore <= 0.4) {
      explanations.add("Unfamiliar device (device score " + String.format("%.2f", r.deviceScore) + ").");
    }

    if (r.behaviorScore >= 0.8) {
      explanations.add("Behaviour matches baseline (behaviour score " + String.format("%.2f", r.behaviorScore) + ").");
    } else if (r.behaviorScore <= 0.4) {
      explanations.add("Behaviour is unusual compared to baseline (behaviour score " + String.format("%.2f", r.behaviorScore) + ").");
    }

    if (r.contextScore >= 0.8) {
      explanations.add("Context is low risk (context score " + String.format("%.2f", r.contextScore) + ").");
    } else if (r.contextScore <= 0.4) {
      explanations.add("Context is higher risk (context score " + String.format("%.2f", r.contextScore) + ").");
    }

    if ("DENY".equalsIgnoreCase(r.decision)) {
      explanations.add("Overall decision is DENY due to elevated risk.");
    } else if ("STEP_UP".equalsIgnoreCase(r.decision)) {
      explanations.add("Overall decision is STEP_UP – additional verification recommended.");
    } else if ("AUTO_LOGIN".equalsIgnoreCase(r.decision)) {
      explanations.add("Overall decision is AUTO_LOGIN – risk low enough for silent login.");
    }

    return new RiskTimelineItem(
        r.id,
        r.createdAt,
        r.decision,
        r.confidence,
        r.behaviorScore,
        r.deviceScore,
        r.contextScore,
        explanations
    );
  }

  @GetMapping("/showcase/devices/history")
  public ResponseEntity<java.util.List<DeviceSummary>> deviceHistory(
      @RequestParam("user_hint") String userHint
  ) {
    var profiles = deviceProfileRepository.findByUser(userHint);
    java.util.List<DeviceSummary> result = profiles.stream()
        .map(this::toDeviceSummary)
        .toList();
    return ResponseEntity.ok(result);
  }

  @GetMapping("/showcase/devices/diff")
  public ResponseEntity<DeviceDiffResponse> deviceDiff(
      @RequestParam("left_id") long leftId,
      @RequestParam("right_id") long rightId
  ) {
    var leftOpt = deviceProfileRepository.findById(leftId);
    var rightOpt = deviceProfileRepository.findById(rightId);
    if (leftOpt.isEmpty() || rightOpt.isEmpty()) {
      return ResponseEntity.notFound().build();
    }
    var left = leftOpt.get();
    var right = rightOpt.get();
    var changes = computeDeviceDiff(left, right);
    return ResponseEntity.ok(new DeviceDiffResponse(
        toDeviceSummary(left),
        toDeviceSummary(right),
        changes
    ));
  }

  @GetMapping("/showcase/behavior/history")
  public ResponseEntity<java.util.List<BehaviorHistoryItem>> behaviorHistory(
      @RequestParam("user_hint") String userHint,
      @RequestParam(name = "limit", defaultValue = "50") int limit
  ) {
    var rows = sessionFeatureRepository.findRecentForUser(userHint, limit);
    java.util.List<BehaviorHistoryItem> result = rows.stream()
        .map(r -> new BehaviorHistoryItem(
            r.id,
            r.occurredAt,
            r.tlsFp,
            r.decision,
            r.confidence,
            r.behaviorJson,
            r.featureVector,
            r.label
        ))
        .toList();
    return ResponseEntity.ok(result);
  }

  @GetMapping("/showcase/risk/timeline")
  public ResponseEntity<java.util.List<RiskTimelineItem>> riskTimeline(
      @RequestParam("user_hint") String userHint,
      @RequestParam(name = "limit", defaultValue = "50") int limit
  ) {
    var rows = decisionLogRepository.findRecentByUser(userHint, limit);
    java.util.List<RiskTimelineItem> result = rows.stream()
        .map(this::toRiskTimelineItem)
        .toList();
    return ResponseEntity.ok(result);
  }

  @GetMapping("/showcase/tls-fp/overview")
  public ResponseEntity<TlsFpOverview> tlsFpOverview(
      @RequestParam("tls_fp") String tlsFp
  ) {
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
          .min(java.time.OffsetDateTime::compareTo)
          .orElse(null);
      row.lastSeen = devices.stream()
          .map(d -> d.lastSeen)
          .filter(java.util.Objects::nonNull)
          .max(java.time.OffsetDateTime::compareTo)
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