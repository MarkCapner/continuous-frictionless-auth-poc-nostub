import React from "react";

export interface DeviceDiffPanelProps {
  userHint: string;
}

/**
 * Placeholder for the device diff viewer.
 *
 * Mini-Epic 8B: layout only – later mini-epics will:
 * - fetch device history for the selected user
 * - allow picking two devices
 * - show field-level diffs and drift metrics
 */
export function DeviceDiffPanel({ userHint }: DeviceDiffPanelProps) {
  return (
    <div style={cardStyle}>
      <h3 style={titleStyle}>Device diff viewer</h3>
      <p style={bodyStyle}>
        This panel will let you pick two devices for <code>{userHint || "demo-user"}</code> and visualise the
        differences in user agent, screen size, timezone, WebGL/canvas hashes, and other device traits.
      </p>
      <p style={placeholderStyle}>Diff UI and data wiring will be added in the next mini-epic.</p>
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
