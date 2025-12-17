
package com.poc.api.risk.drift.temporal;

import java.time.OffsetDateTime;
import java.util.*;

public class TemporalDriftService {

    public DriftTrend analyze(List<SessionDriftPoint> points) {
        points.sort(Comparator.comparing(SessionDriftPoint::timestamp));
        double first = points.isEmpty() ? 0 : points.get(0).totalDrift();
        double last = points.isEmpty() ? 0 : points.get(points.size()-1).totalDrift();
        String trend = last < first ? "RECOVERING" : last > first ? "WORSENING" : "STABLE";
        return new DriftTrend(trend, first, last);
    }

    public record SessionDriftPoint(long sessionId, OffsetDateTime timestamp, double totalDrift) {}
    public record DriftTrend(String trend, double startDrift, double endDrift) {}
}
