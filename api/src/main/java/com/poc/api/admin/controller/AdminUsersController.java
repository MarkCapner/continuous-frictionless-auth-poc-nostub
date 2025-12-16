package com.poc.api.admin.controller;

import com.poc.api.risk.persistence.DecisionLogRepository;
import com.poc.api.risk.persistence.UserSummaryRow;
import com.poc.api.risk.service.AccountSharingHeuristics;
import com.poc.api.risk.service.UserReputationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Admin endpoints for user-level intelligence and reputation (EPIC 6).
 *
 * These are intentionally simple and unauthenticated for the PoC.
 */
@RestController
@org.springframework.web.bind.annotation.RequestMapping({"/api","/api/v1"})
public class AdminUsersController {

  private final DecisionLogRepository decisionLogRepository;
  private final UserReputationService userReputationService;
  private final AccountSharingHeuristics accountSharingHeuristics;

  public AdminUsersController(DecisionLogRepository decisionLogRepository,
                              UserReputationService userReputationService,
                              AccountSharingHeuristics accountSharingHeuristics) {
    this.decisionLogRepository = decisionLogRepository;
    this.userReputationService = userReputationService;
    this.accountSharingHeuristics = accountSharingHeuristics;
  }

  public record AdminUserSummary(
      String userId,
      long sessions,
      long devices,
      java.time.OffsetDateTime lastSeen,
      double avgConfidence,
      double userTrustScore,
      double userAccountSharingRisk
  ) {}

  public record AdminUserDetail(
      String userId,
      UserReputationService.Reputation reputation,
      AccountSharingHeuristics.Result sharing
  ) {}

  @GetMapping("/admin/users")
  public ResponseEntity<List<AdminUserSummary>> listUsers(
      @RequestParam(name = "limit", defaultValue = "50") int limit
  ) {
    List<UserSummaryRow> rows = decisionLogRepository.findUserSummaries(limit);
    List<AdminUserSummary> result = rows.stream()
        .map(row -> {
          var rep = userReputationService.evaluate(row.userId);
          return new AdminUserSummary(
              row.userId,
              row.sessions,
              row.devices,
              row.lastSeen,
              row.avgConfidence,
              rep.trustScore(),
              rep.accountSharingRisk()
          );
        })
        .toList();
    return ResponseEntity.ok(result);
  }

  @GetMapping("/admin/users/{userId}")
  public ResponseEntity<AdminUserDetail> userDetail(@PathVariable("userId") String userId) {
    var rep = userReputationService.evaluate(userId);
    var sharing = accountSharingHeuristics.evaluate(userId);
    return ResponseEntity.ok(new AdminUserDetail(userId, rep, sharing));
  }
}
