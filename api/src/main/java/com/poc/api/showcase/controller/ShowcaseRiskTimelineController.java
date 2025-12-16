package com.poc.api.showcase.controller;

import com.poc.api.risk.persistence.DecisionLogRepository;
import com.poc.api.risk.persistence.DecisionLogRow;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping({"/api/showcase/risk","/api/v1/showcase/risk"})
public class ShowcaseRiskTimelineController {

  private final DecisionLogRepository decisionLogRepository;

  public ShowcaseRiskTimelineController(DecisionLogRepository decisionLogRepository) {
    this.decisionLogRepository = decisionLogRepository;
  }

  public record RiskTimelineItem(
      Long id,
      OffsetDateTime occurredAt,
      String decision,
      double confidence,
      double behaviorScore,
      double deviceScore,
      double contextScore,
      List<String> explanations
  ) {}

  @GetMapping("/timeline")
  public ResponseEntity<List<RiskTimelineItem>> riskTimeline(
      @RequestParam("user_hint") String userHint,
      @RequestParam(name = "limit", defaultValue = "50") int limit
  ) {
    var rows = decisionLogRepository.findRecentByUser(userHint, limit);
    List<RiskTimelineItem> result = rows.stream().map(this::toRiskTimelineItem).toList();
    return ResponseEntity.ok(result);
  }

  private RiskTimelineItem toRiskTimelineItem(DecisionLogRow r) {
    List<String> explanations = new ArrayList<>();

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
}
