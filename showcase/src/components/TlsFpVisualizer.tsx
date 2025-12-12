import React from "react";

export interface TlsFpVisualizerProps {
  userHint: string;
}

/**
 * Placeholder for the TLS fingerprint visualiser.
 *
 * Mini-Epic 8B: layout only – later mini-epics will:
 * - fetch TLS fingerprint overview for the selected user or chosen fingerprint
 * - display rarity metrics and component fields (JA3/JA4-style breakdowns)
 */
export function TlsFpVisualizer({ userHint }: TlsFpVisualizerProps) {
  return (
    <div style={cardStyle}>
      <h3 style={titleStyle}>TLS fingerprint visualiser</h3>
      <p style={bodyStyle}>
        This panel will show details for TLS fingerprints seen for <code>{userHint || "demo-user"}</code>, including
        rarity, first/last seen, and a breakdown of fingerprint components.
      </p>
      <p style={placeholderStyle}>Visualisation of TLS fingerprint structure will be added in a later mini-epic.</p>
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
