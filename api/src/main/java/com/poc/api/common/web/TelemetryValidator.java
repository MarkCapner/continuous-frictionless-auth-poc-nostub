package com.poc.api.common.web;

import com.poc.api.telemetry.dto.Telemetry;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class TelemetryValidator {

  private static final String[] BANNED_KEYS = new String[] {
      "email",
      "e-mail",
      "phone",
      "phone_number",
      "name",
      "full_name",
      "first_name",
      "last_name",
      "dob",
      "date_of_birth",
      "ssn",
      "passport",
      "national_id"
  };

  public void validate(Telemetry telemetry) {
    if (telemetry == null) {
      return;
    }
    Map<String, Object> ctx = telemetry.context();
    if (ctx == null || ctx.isEmpty()) {
      return;
    }

    List<String> offendingKeys = new ArrayList<>();
    for (String key : ctx.keySet()) {
      String lower = key.toLowerCase(Locale.ROOT);
      for (String banned : BANNED_KEYS) {
        if (lower.equals(banned)) {
          offendingKeys.add(key);
          break;
        }
      }
    }

    if (!offendingKeys.isEmpty()) {
      throw new TelemetryValidationException(
          "PII-like keys are not allowed in telemetry context: " + String.join(", ", offendingKeys)
      );
    }
  }
}
