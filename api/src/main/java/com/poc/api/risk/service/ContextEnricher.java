package com.poc.api.risk.service;

import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class ContextEnricher {

  public Map<String, Object> enrich(String ip, Map<String, Object> rawContext) {
    Map<String, Object> ctx = new LinkedHashMap<>();
    if (rawContext != null) {
      ctx.putAll(rawContext);
    }

    OffsetDateTime now = OffsetDateTime.now();
    int hour = now.getHour();
    DayOfWeek dow = now.getDayOfWeek();
    boolean weekend = (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY);

    // server-derived temporal context always wins over client hints
    ctx.put("hour", hour);
    ctx.put("dow", dow.getValue());
    ctx.put("weekend", weekend);

    // expose IP in context for downstream consumers; we do not compute ASN/ip_stability here
    if (ip != null && !ip.isBlank()) {
      ctx.putIfAbsent("ip", ip);
    }

    return ctx;
  }
}
