package com.poc.api.risk.explainability;

import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class SessionExplainabilityRepository {
  private final Map<String, SessionExplainability> store = new HashMap<>();

  public void save(SessionExplainability s) {
    store.put(s.getSessionId(), s);
  }

  public Optional<SessionExplainability> findBySessionId(String id) {
    return Optional.ofNullable(store.get(id));
  }

  public Collection<SessionExplainability> findAll() {
    return store.values();
  }
}
