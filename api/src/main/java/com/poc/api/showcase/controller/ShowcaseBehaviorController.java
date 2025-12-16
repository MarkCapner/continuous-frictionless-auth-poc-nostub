package com.poc.api.showcase.controller;

import com.poc.api.risk.persistence.SessionFeatureRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/showcase/behavior")
public class ShowcaseBehaviorController {

  private final SessionFeatureRepository sessionFeatureRepository;

  public ShowcaseBehaviorController(SessionFeatureRepository sessionFeatureRepository) {
    this.sessionFeatureRepository = sessionFeatureRepository;
  }

  public record BehaviorHistoryItem(
      Long id,
      OffsetDateTime occurredAt,
      String tlsFp,
      String decision,
      double confidence,
      String behaviorJson,
      String featureVector,
      String label
  ) {}

  @GetMapping("/history")
  public ResponseEntity<List<BehaviorHistoryItem>> behaviorHistory(
      @RequestParam("user_hint") String userHint,
      @RequestParam(name = "limit", defaultValue = "50") int limit
  ) {
    var rows = sessionFeatureRepository.findRecentForUser(userHint, limit);
    List<BehaviorHistoryItem> result = rows.stream()
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
}
