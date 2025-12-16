import React, { useState } from "react";

type Props = {
  decision: string;
  confidence: number;
  topPositive?: any[];
  topNegative?: any[];
};

export const WhyDecisionPanel: React.FC<Props> = ({
  decision,
  confidence,
  topPositive = [],
  topNegative = []
}) => {
  const [expanded, setExpanded] = useState(false);

  return (
    <div style={{ border: "1px solid #ddd", borderRadius: 8, padding: 16 }}>
      <h3>Why this decision?</h3>
      <p>
        <strong>{decision}</strong> (confidence {(confidence * 100).toFixed(1)}%)
      </p>

      <div style={{ display: "flex", gap: 16 }}>
        <div style={{ flex: 1 }}>
          <h4>Positive signals</h4>
          {topPositive.length === 0 && <em>None</em>}
          {topPositive.map((p, i) => (
            <div key={i}>+ {p.key ?? p.feature} ({p.contribution})</div>
          ))}
        </div>

        <div style={{ flex: 1 }}>
          <h4>Negative signals</h4>
          {topNegative.length === 0 && <em>None</em>}
          {topNegative.map((n, i) => (
            <div key={i}>- {n.key ?? n.feature} ({n.contribution})</div>
          ))}
        </div>
      </div>

      <button onClick={() => setExpanded(!expanded)} style={{ marginTop: 12 }}>
        {expanded ? "Hide details" : "Show details"}
      </button>

      {expanded && (
        <pre style={{ marginTop: 12, background: "#fafafa", padding: 8 }}>
          {JSON.stringify({ topPositive, topNegative }, null, 2)}
        </pre>
      )}
    </div>
  );
};
