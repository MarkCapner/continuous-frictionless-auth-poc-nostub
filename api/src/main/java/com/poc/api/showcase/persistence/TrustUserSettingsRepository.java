package com.poc.api.showcase.persistence;

import com.poc.api.showcase.dto.TrustUserSettings;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;

@Repository
public class TrustUserSettingsRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<TrustUserSettings> mapper = (rs, rowNum) -> {
        TrustUserSettings s = new TrustUserSettings();
        s.userId = rs.getString("user_id");
        s.consentGranted = rs.getBoolean("consent_granted");
        s.consentUpdatedAt = rs.getObject("consent_updated_at", OffsetDateTime.class);
        s.baselineResetAt = rs.getObject("baseline_reset_at", OffsetDateTime.class);
        s.baselineResetReason = rs.getString("baseline_reset_reason");
        return s;
    };

    public TrustUserSettingsRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<TrustUserSettings> findByUserId(String userId) {
        try {
            TrustUserSettings s = jdbcTemplate.queryForObject(
                    "select user_id, consent_granted, consent_updated_at, baseline_reset_at, baseline_reset_reason " +
                            "from trust_user_settings where user_id = ?",
                    mapper,
                    userId
            );
            return Optional.ofNullable(s);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public TrustUserSettings upsertConsent(String userId, boolean consentGranted) {
        jdbcTemplate.update(
                "insert into trust_user_settings (user_id, consent_granted, consent_updated_at) " +
                        "values (?, ?, now()) " +
                        "on conflict (user_id) do update set consent_granted = excluded.consent_granted, consent_updated_at = now()",
                userId,
                consentGranted
        );
        return findByUserId(userId).orElseGet(() -> {
            TrustUserSettings s = new TrustUserSettings();
            s.userId = userId;
            s.consentGranted = consentGranted;
            s.consentUpdatedAt = OffsetDateTime.now();
            return s;
        });
    }

    public TrustUserSettings markBaselineReset(String userId, String reason) {
        jdbcTemplate.update(
                "insert into trust_user_settings (user_id, baseline_reset_at, baseline_reset_reason) " +
                        "values (?, now(), ?) " +
                        "on conflict (user_id) do update set baseline_reset_at = now(), baseline_reset_reason = excluded.baseline_reset_reason",
                userId,
                reason
        );
        return findByUserId(userId).orElseGet(() -> {
            TrustUserSettings s = new TrustUserSettings();
            s.userId = userId;
            s.consentGranted = true;
            s.baselineResetAt = OffsetDateTime.now();
            s.baselineResetReason = reason;
            return s;
        });
    }
}
