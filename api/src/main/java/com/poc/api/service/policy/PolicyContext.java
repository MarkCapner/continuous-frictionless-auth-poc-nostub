package com.poc.api.service.policy;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Immutable policy evaluation context.
 */
public class PolicyContext {

    private final Map<String, Object> values;

    public PolicyContext(Map<String, Object> values) {
        this.values = values == null ? new LinkedHashMap<>() : new LinkedHashMap<>(values);
    }

    public Map<String, Object> values() {
        return Collections.unmodifiableMap(values);
    }
}
