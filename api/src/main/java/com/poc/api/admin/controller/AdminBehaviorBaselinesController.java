package com.poc.api.admin.controller;

import com.poc.api.telemetry.persistence.BehaviorBaselineRow;
import com.poc.api.telemetry.persistence.BehaviorStatRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/behavior")
public class AdminBehaviorBaselinesController {

  private final BehaviorStatRepository behaviorStatRepository;

  public AdminBehaviorBaselinesController(BehaviorStatRepository behaviorStatRepository) {
    this.behaviorStatRepository = behaviorStatRepository;
  }

  @GetMapping("/baselines")
  public ResponseEntity<List<BehaviorBaselineRow>> behaviorBaselines(
      @RequestParam(name = "limit", defaultValue = "200") int limit
  ) {
    var stats = behaviorStatRepository.findRecent(limit);
    List<BehaviorBaselineRow> rows = stats.stream().map(s -> {
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
}
