package com.poc.api.risk.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class RetentionService {

  private static final Logger log = LoggerFactory.getLogger(RetentionService.class);

  private final JdbcTemplate jdbcTemplate;
  private final boolean retentionEnabled;
  private final int retentionDays;

  public RetentionService(JdbcTemplate jdbcTemplate,
                          @Value("${retention.enabled:true}") boolean retentionEnabled,
                          @Value("${retention.days:30}") int retentionDays) {
    this.jdbcTemplate = jdbcTemplate;
    this.retentionEnabled = retentionEnabled;
    this.retentionDays = retentionDays;
  }

  /**
   * Purge old session and decision records once a day at 03:00.
   */
  @Scheduled(cron = "0 0 3 * * *")
  public void purgeOldData() {
    if (!retentionEnabled) {
      return;
    }
    if (retentionDays <= 0) {
      return;
    }

    int days = retentionDays;

    int deletedSessions = jdbcTemplate.update(
        "DELETE FROM session_feature WHERE occurred_at < now() - (? * INTERVAL '1 day')",
        days
    );
    int deletedDecisions = jdbcTemplate.update(
        "DELETE FROM decision_log WHERE created_at < now() - (? * INTERVAL '1 day')",
        days
    );

    if (deletedSessions > 0 || deletedDecisions > 0) {
      log.info("Retention job purged {} session_feature rows and {} decision_log rows older than {} days",
          deletedSessions, deletedDecisions, days);
    }
  }
}
