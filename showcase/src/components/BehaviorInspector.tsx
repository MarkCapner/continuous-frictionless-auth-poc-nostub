import React from "react";

export interface BehaviorInspectorProps {
  userHint: string;
}

/**
 * Placeholder for the behavioural inspector.
 *
 * Mini-Epic 8B: layout only – later mini-epics will:
 * - fetch behaviour history for the selected user
 * - plot keystroke timings, scroll cadence, pointer velocity
 * - show per-feature z-score timelines
 */
export function BehaviorInspector({ userHint }: BehaviorInspectorProps) {
  return (
    <div style={cardStyle}>
      <h3 style={titleStyle}>Behavioural inspector</h3>
      <p style={bodyStyle}>
        This panel will show per-session behavioural features for <code>{userHint || "demo-user"}</code>, including
        keystroke timing aggregates, scroll cadence, pointer velocity segments, and z-score deltas vs baseline.
      </p>
      <p style={placeholderStyle}>Charts and feature timelines will be wired in a later mini-epic.</p>
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
