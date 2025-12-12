import React, { useEffect, useState } from "react";
import type { RiskTimelineItem as RiskTimelineItemDto } from "../api";
import { fetchRiskTimeline } from "../api";

export interface RiskTimelineProps {
  userHint: string;
}

export function RiskTimeline({ userHint }: RiskTimelineProps) {
  const [items, setItems] = useState<RiskTimelineItemDto[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    const run = async () => {
      const hint = (userHint || "demo-user").trim() || "demo-user";
      setLoading(true);
      setError(null);
      setItems([]);
      try {
        const data = await fetchRiskTimeline(hint, 50);
        if (!cancelled) {
          // backend returns newest-first; timeline UX is usually newest at top, so keep as-is
          setItems(data);
        }
      } catch (e: any) {
        if (!cancelled) setError(e?.message ?? String(e));
      } finally {
        if (!cancelled) setLoading(false);
      }
    };
    void run();
    return () => {
      cancelled = true;
    };
  }, [userHint]);

  const summary = computeSummary(items);

  return (
    <div style={cardStyle}>
      <h3 style={titleStyle}>Risk explanation timeline</h3>
      <p style={bodyStyle}>
        Per-session risk decisions and scores for{" "}
        <code>{(userHint || "demo-user").trim() || "demo-user"}</code>. Each event shows the decision, confidence,
        risk scores, and the backend-generated explanation bullets.
      </p>
      {loading && <p style={hintStyle}>Loading risk timeline…</p>}
      {error && (
        <p style={{ ...hintStyle, color: "#b91c1c" }}>Error loading timeline: {error}</p>
      )}
      {!loading && !error && items.length === 0 && (
        <p style={hintStyle}>No decisions recorded yet for this user. Run a few profile checks first.</p>
      )}

      {!loading && !error && items.length > 0 && (
        <>
          <TimelineSummary {...summary} />
          <div style={timelineContainerStyle}>
            <div style={timelineRailStyle} />
            <div style={timelineListStyle}>
              {items.map((item) => (
                <TimelineEvent key={item.id} item={item} />
              ))}
            </div>
          </div>
        </>
      )}
    </div>
  );
}

interface Summary {
  total: number;
  autoLogin: number;
  stepUp: number;
  deny: number;
  avgConfidence: number;
}

function computeSummary(items: RiskTimelineItemDto[]): Summary {
  if (items.length === 0) {
    return { total: 0, autoLogin: 0, stepUp: 0, deny: 0, avgConfidence: 0 };
  }
  let auto = 0;
  let step = 0;
  let deny = 0;
  let sumConf = 0;
  for (const i of items) {
    if (i.decision === "AUTO_LOGIN") auto++;
    else if (i.decision === "STEP_UP") step++;
    else if (i.decision === "DENY") deny++;
    sumConf += i.confidence;
  }
  return {
    total: items.length,
    autoLogin: auto,
    stepUp: step,
    deny,
    avgConfidence: sumConf / items.length
  };
}

interface TimelineSummaryProps extends Summary {}

function TimelineSummary({
  total,
  autoLogin,
  stepUp,
  deny,
  avgConfidence
}: TimelineSummaryProps) {
  if (!total) return null;
  return (
    <div style={summaryRowStyle}>
      <span style={summaryItemStyle}>
        <strong>{total}</strong> events
      </span>
      <span style={summaryItemStyle}>
        <ColourDot colour="#16a34a" /> AUTO_LOGIN <strong>{autoLogin}</strong>
      </span>
      <span style={summaryItemStyle}>
        <ColourDot colour="#f97316" /> STEP_UP <strong>{stepUp}</strong>
      </span>
      <span style={summaryItemStyle}>
        <ColourDot colour="#dc2626" /> DENY <strong>{deny}</strong>
      </span>
      <span style={summaryItemStyle}>
        Avg confidence <strong>{(avgConfidence * 100).toFixed(1)}%</strong>
      </span>
    </div>
  );
}

interface TimelineEventProps {
  item: RiskTimelineItemDto;
}

function TimelineEvent({ item }: TimelineEventProps) {
  const colour = decisionColour(item.decision);
  return (
    <div style={eventRowStyle}>
      <div style={eventBulletWrapperStyle}>
        <div
          style={{
            ...eventBulletStyle,
            background: colour
          }}
        />
      </div>
      <div style={eventCardStyle}>
        <div style={eventHeaderStyle}>
          <span style={eventDecisionStyle}>
            <DecisionTag decision={item.decision} />
            <span style={{ marginLeft: 8, fontSize: "0.8rem", color: "#6b7280" }}>
              {(item.confidence * 100).toFixed(1)}% confidence
            </span>
          </span>
          <span style={eventTimeStyle}>{formatDateTime(item.occurredAt)}</span>
        </div>
        <div style={scoreRowStyle}>
          <ScorePill label="Behaviour" value={item.behaviorScore} />
          <ScorePill label="Device" value={item.deviceScore} />
          <ScorePill label="Context" value={item.contextScore} />
        </div>
        {item.explanations && item.explanations.length > 0 && (
          <ul style={explanationsListStyle}>
            {item.explanations.map((e, idx) => (
              <li key={idx}>{e}</li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}

interface ScorePillProps {
  label: string;
  value: number;
}

function ScorePill({ label, value }: ScorePillProps) {
  const pct = Math.round(value * 100);
  const colour = value >= 0.8 ? "#16a34a" : value <= 0.4 ? "#dc2626" : "#f97316";
  return (
    <span style={scorePillStyle}>
      <span
        style={{
          display: "inline-block",
          width: 8,
          height: 8,
          borderRadius: 999,
          marginRight: 6,
          background: colour
        }}
      />
      {label}: {pct}%
    </span>
  );
}

interface ColourDotProps {
  colour: string;
}

function ColourDot({ colour }: ColourDotProps) {
  return (
    <span
      style={{
        display: "inline-block",
        width: 8,
        height: 8,
        borderRadius: 999,
        background: colour,
        marginRight: 4
      }}
    />
  );
}

interface DecisionTagProps {
  decision: string;
}

function DecisionTag({ decision }: DecisionTagProps) {
  const colour = decisionColour(decision);
  return (
    <span style={{ ...decisionTagStyle, color: colour, borderColor: colour }}>
      <span
        style={{
          display: "inline-block",
          width: 8,
          height: 8,
          borderRadius: 999,
          marginRight: 6,
          background: colour
        }}
      />
      {decision}
    </span>
  );
}

function decisionColour(decision: string): string {
  if (decision === "AUTO_LOGIN") return "#16a34a";
  if (decision === "STEP_UP") return "#f97316";
  if (decision === "DENY") return "#dc2626";
  return "#6b7280";
}

function formatDateTime(iso: string): string {
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  return d.toLocaleString();
}

const cardStyle: React.CSSProperties = {
  borderRadius: 8,
  borderWidth: 1,
  borderStyle: "solid",
  borderColor: "#e5e7eb",
  background: "#fff",
  padding: "0.75rem 1rem",
  minHeight: 220,
  display: "flex",
  flexDirection: "column"
};

const titleStyle: React.CSSProperties = {
  margin: "0 0 0.35rem"
};

const bodyStyle: React.CSSProperties = {
  margin: "0.25rem 0"
};

const hintStyle: React.CSSProperties = {
  margin: "0.25rem 0",
  fontSize: "0.8rem",
  color: "#6b7280"
};

const summaryRowStyle: React.CSSProperties = {
  display: "flex",
  flexWrap: "wrap",
  gap: "0.5rem",
  margin: "0.5rem 0 0.5rem"
};

const summaryItemStyle: React.CSSProperties = {
  fontSize: "0.8rem",
  color: "#4b5563"
};

const timelineContainerStyle: React.CSSProperties = {
  position: "relative",
  marginTop: "0.5rem",
  paddingLeft: 12,
  maxHeight: 260,
  overflowY: "auto"
};

const timelineRailStyle: React.CSSProperties = {
  position: "absolute",
  left: 5,
  top: 0,
  bottom: 0,
  width: 2,
  background: "#e5e7eb"
};

const timelineListStyle: React.CSSProperties = {
  display: "flex",
  flexDirection: "column",
  gap: "0.5rem"
};

const eventRowStyle: React.CSSProperties = {
  display: "flex",
  alignItems: "flex-start",
  gap: "0.5rem"
};

const eventBulletWrapperStyle: React.CSSProperties = {
  marginTop: 8
};

const eventBulletStyle: React.CSSProperties = {
  width: 10,
  height: 10,
  borderRadius: 999
};

const eventCardStyle: React.CSSProperties = {
  flex: 1,
  borderRadius: 8,
  borderWidth: 1,
  borderStyle: "solid",
  borderColor: "#e5e7eb",
  background: "#f9fafb",
  padding: "0.5rem 0.75rem"
};

const eventHeaderStyle: React.CSSProperties = {
  display: "flex",
  justifyContent: "space-between",
  alignItems: "baseline",
  gap: "0.5rem"
};

const eventDecisionStyle: React.CSSProperties = {
  display: "flex",
  alignItems: "center",
  gap: 4
};

const eventTimeStyle: React.CSSProperties = {
  fontSize: "0.75rem",
  color: "#6b7280"
};

const scoreRowStyle: React.CSSProperties = {
  display: "flex",
  flexWrap: "wrap",
  gap: "0.5rem",
  marginTop: 4
};

const scorePillStyle: React.CSSProperties = {
  fontSize: "0.75rem",
  padding: "0.1rem 0.4rem",
  borderRadius: 999,
  borderWidth: 1,
  borderStyle: "solid",
  borderColor: "#e5e7eb",
  background: "#fff"
};

const explanationsListStyle: React.CSSProperties = {
  margin: "0.35rem 0 0",
  paddingLeft: "1.1rem",
  fontSize: "0.8rem",
  color: "#374151"
};

const decisionTagStyle: React.CSSProperties = {
  fontSize: "0.75rem",
  padding: "0.1rem 0.45rem",
  borderRadius: 999,
  borderWidth: 1,
  borderStyle: "solid",
  borderColor: "#9ca3af",
  background: "#fff",
  display: "inline-flex",
  alignItems: "center"
};
