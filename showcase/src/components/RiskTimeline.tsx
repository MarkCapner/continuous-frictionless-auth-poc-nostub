import React from "react";

export interface RiskTimelineProps {
  userHint: string;
}

/**
 * Placeholder for the risk explanation timeline.
 *
 * Mini-Epic 8B: layout only – later mini-epics will:
 * - fetch risk timeline events for the selected user
 * - show AUTO_LOGIN / STEP_UP / DENY events with confidence and anomaly scores
 * - render explanations in a vertical timeline
 */
export function RiskTimeline({ userHint }: RiskTimelineProps) {
  return (
    <div style={cardStyle}>
      <h3 style={titleStyle}>Risk explanation timeline</h3>
      <p style={bodyStyle}>
        This panel will plot a vertical timeline of risk decisions for <code>{userHint || "demo-user"}</code>,
        including decision labels, confidence, ML anomaly scores, and human-readable explanation bullets.
      </p>
      <p style={placeholderStyle}>Timeline events and colours will be added in a later mini-epic.</p>
    </div>
  );
}

const cardStyle: React.CSSProperties = {
  borderRadius: 8,
  borderWidth: 1,
  borderStyle: "solid",
  borderColor: "#e5e7eb",
  background: "#fff",
  padding: "0.75rem 1rem",
  fontSize: "0.85rem"
};

const titleStyle: React.CSSProperties = {
  margin: "0 0 0.35rem"
};

const bodyStyle: React.CSSProperties = {
  margin: "0.25rem 0"
};

const placeholderStyle: React.CSSProperties = {
  margin: "0.25rem 0 0",
  fontStyle: "italic",
  color: "#6b7280"
};
