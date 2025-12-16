package com.poc.api.admin.controller;

import com.poc.api.common.persistence.AdminAnalyticsRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * EPIC 7: admin analytics APIs for user/session/model/risk statistics.
 *
 * These endpoints are read-only and are intended for dashboards / admin tooling.
 */
@RestController
@RequestMapping("/api/admin/analytics")
public class AdminAnalyticsController {

  private final AdminAnalyticsRepository analyticsRepository;

  public AdminAnalyticsController(AdminAnalyticsRepository analyticsRepository) {
    this.analyticsRepository = analyticsRepository;
  }

  @GetMapping("/risk")
  public ResponseEntity<List<AdminAnalyticsRepository.RiskStatsRow>> riskStats() {
    return ResponseEntity.ok(analyticsRepository.findRiskStats());
  }

  @GetMapping("/sessions/daily")
  public ResponseEntity<List<AdminAnalyticsRepository.SessionDailyStatsRow>> sessionDaily(
      @RequestParam(name = "limit", defaultValue = "30") int limit
  ) {
    return ResponseEntity.ok(analyticsRepository.findSessionDailyStats(limit));
  }

  @GetMapping("/model/confusion")
  public ResponseEntity<List<AdminAnalyticsRepository.ModelConfusionRow>> modelConfusion() {
    return ResponseEntity.ok(analyticsRepository.findModelConfusion());
  }
}
