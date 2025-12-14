package com.poc.api.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public class DriftRepository {

  private final JdbcTemplate jdbc;

  public DriftRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public record DriftBaselineRow(
      String userId,
      OffsetDateTime updatedAt,
      String lastDeviceSig,
      String lastTlsFamily,
      String lastModelVersion,
      long confCount,
      double confMean,
      double confM2
  ) {}

  public DriftBaselineRow getBaseline(String userId) {
    List<DriftBaselineRow> rows = jdbc.query(
        "SELECT * FROM drift_baseline WHERE user_id = ?",
        (rs, i) -> new DriftBaselineRow(
            rs.getString("user_id"),
            rs.getObject("updated_at", OffsetDateTime.class),
            rs.getString("last_device_sig"),
            rs.getString("last_tls_family"),
            rs.getString("last_model_version"),
            rs.getLong("conf_count"),
            rs.getDouble("conf_mean"),
            rs.getDouble("conf_m2")
        ),
        userId
    );
    return rows.isEmpty() ? null : rows.get(0);
  }

  public void upsertBaseline(DriftBaselineRow b) {
    jdbc.update("""
      INSERT INTO drift_baseline(user_id, updated_at, last_device_sig, last_tls_family, last_model_version,
                                 conf_count, conf_mean, conf_m2)
      VALUES (?,?,?,?,?,?,?,?)
      ON CONFLICT (user_id) DO UPDATE SET
        updated_at = EXCLUDED.updated_at,
        last_device_sig = EXCLUDED.last_device_sig,
        last_tls_family = EXCLUDED.last_tls_family,
        last_model_version = EXCLUDED.last_model_version,
        conf_count = EXCLUDED.conf_count,
        conf_mean = EXCLUDED.conf_mean,
        conf_m2 = EXCLUDED.conf_m2
      """,
      b.userId(), b.updatedAt(), b.lastDeviceSig(), b.lastTlsFamily(), b.lastModelVersion(),
      b.confCount(), b.confMean(), b.confM2()
    );
  }

  public void insertEvent(String userId, String requestId,
                          double deviceDrift, double behaviorDrift, double tlsDrift,
                          double featureDrift, double modelInstability, double maxDrift,
                          String warningsJson) {
    jdbc.update("""
      INSERT INTO drift_event(user_id, request_id, device_drift, behavior_drift, tls_drift, feature_drift, model_instability, max_drift, warnings)
      VALUES (?,?,?,?,?,?,?,?, ?::jsonb)
      """,
      userId, requestId, deviceDrift, behaviorDrift, tlsDrift, featureDrift, modelInstability, maxDrift, warningsJson
    );
  }
}
