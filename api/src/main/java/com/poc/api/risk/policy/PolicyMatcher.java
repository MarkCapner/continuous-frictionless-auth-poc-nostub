package com.poc.api.risk.policy;

import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * Minimal deterministic condition matcher for PolicyRules.
 *
 * Condition JSON supports:
 * - equality: { "device.new": true }
 * - numeric compares: { "drift.score": {"gt": 0.7} }
 * - in list: { "country": {"in": ["GB","US"]} }
 * - contains (string/collection): { "user.roles": {"contains": "VIP"} }
 */
public final class PolicyMatcher {

    private PolicyMatcher() {}

    public static boolean matches(Map<String, Object> condition, Map<String, Object> ctx) {
        if (condition == null || condition.isEmpty()) return false;
        for (Map.Entry<String, Object> e : condition.entrySet()) {
            String key = e.getKey();
            Object expected = e.getValue();
            Object actual = ctx.get(key);

            if (expected instanceof Map<?,?> ops) {
                if (!matchOps(actual, (Map<?,?>) ops)) return false;
            } else {
                if (!Objects.equals(normaliseScalar(actual), normaliseScalar(expected))) return false;
            }
        }
        return true;
    }

    private static boolean matchOps(Object actual, Map<?,?> ops) {
        if (ops.isEmpty()) return false;

        // handle numeric comparisons
        if (ops.containsKey("gt") || ops.containsKey("gte") || ops.containsKey("lt") || ops.containsKey("lte")) {
            Double a = toDouble(actual);
            if (a == null) return false;
            if (ops.containsKey("gt"))  return a >  toDouble(ops.get("gt"));
            if (ops.containsKey("gte")) return a >= toDouble(ops.get("gte"));
            if (ops.containsKey("lt"))  return a <  toDouble(ops.get("lt"));
            if (ops.containsKey("lte")) return a <= toDouble(ops.get("lte"));
        }

        if (ops.containsKey("eq")) {
            Object exp = ops.get("eq");
            return Objects.equals(normaliseScalar(actual), normaliseScalar(exp));
        }
        if (ops.containsKey("neq")) {
            Object exp = ops.get("neq");
            return !Objects.equals(normaliseScalar(actual), normaliseScalar(exp));
        }

        if (ops.containsKey("in")) {
            Object list = ops.get("in");
            if (list instanceof Collection<?> col) {
                Object normA = normaliseScalar(actual);
                for (Object o : col) {
                    if (Objects.equals(normA, normaliseScalar(o))) return true;
                }
                return false;
            }
        }

        if (ops.containsKey("contains")) {
            Object needle = ops.get("contains");
            if (actual instanceof String s) {
                String n = needle == null ? "" : needle.toString();
                return StringUtils.hasText(n) && s.contains(n);
            }
            if (actual instanceof Collection<?> col) {
                for (Object o : col) {
                    if (Objects.equals(normaliseScalar(o), normaliseScalar(needle))) return true;
                }
                return false;
            }
            return false;
        }

        return false;
    }

    private static Object normaliseScalar(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.doubleValue();
        if (v instanceof Boolean b) return b;
        return v.toString();
    }

    private static Double toDouble(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.doubleValue();
        if (v instanceof String s) {
            try { return Double.parseDouble(s); } catch (Exception ignored) {}
        }
        return null;
    }
}
