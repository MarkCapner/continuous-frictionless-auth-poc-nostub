package com.poc.api.showcase.controller;

import com.poc.api.risk.persistence.DecisionLogRepository;
import com.poc.api.risk.persistence.DecisionLogRow;
import com.poc.api.risk.persistence.UserSummaryRow;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/showcase")
public class ShowcaseUsersController {

  private final DecisionLogRepository decisionLogRepository;

  public ShowcaseUsersController(DecisionLogRepository decisionLogRepository) {
    this.decisionLogRepository = decisionLogRepository;
  }

  @GetMapping("/users")
  public ResponseEntity<List<UserSummaryRow>> users(
      @RequestParam(name = "limit", defaultValue = "20") int limit
  ) {
    return ResponseEntity.ok(decisionLogRepository.findUserSummaries(limit));
  }

  @GetMapping("/sessions")
  public ResponseEntity<List<DecisionLogRow>> sessions(
      @RequestParam("user_hint") String userHint,
      @RequestParam(name = "limit", defaultValue = "20") int limit
  ) {
    return ResponseEntity.ok(decisionLogRepository.findRecentByUser(userHint, limit));
  }
}
