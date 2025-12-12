import React from "react";

export interface DeviceHistoryChartsProps {
  userHint: string;
}

/**
 * Placeholder for device history charts.
 *
 * Mini-Epic 8B: layout only – later mini-epics will:
 * - fetch device history for the selected user
 * - plot drift scores, fingerprint changes, and stability metrics over time
 */
export function DeviceHistoryCharts({ userHint }: DeviceHistoryChartsProps) {
  return (
    <div style={cardStyle}>
      <h3 style={titleStyle}>Device history</h3>
      <p style={bodyStyle}>
        This panel will chart device history for <code>{userHint || "demo-user"}</code>, showing how device profiles
        and TLS fingerprints evolve over time and how often each device is seen.
      </p>
      <p style={placeholderStyle}>Charts and per-device breakdowns will be wired later.</p>
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
