package com.poc.api.risk.policy.dto;

import java.time.OffsetDateTime;
import java.util.Map;

public record PolicyMatchEvent(
    String session_id,
    String user_id,
    String decision,
    double confidence,
    OffsetDateTime occurred_at,
    Map<String, Object> policy
) {}
