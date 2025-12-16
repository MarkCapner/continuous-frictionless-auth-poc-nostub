import React from "react";

type Drift = {
  feature: string;
  driftScore: number;
  level: string;
  explanation: string;
};

type Props = {
  drifts?: Drift[];
};

export const FeatureDriftPanel: React.FC<Props> = ({ drifts = [] }) => {
  if (drifts.length === 0) {
    return <em>No drift data available.</em>;
  }

  return (
    <div style={{ border: "1px solid #eee", borderRadius: 8, padding: 16, marginTop: 16 }}>
      <h3>Feature stability & drift</h3>
      {drifts.map((d, i) => (
        <div key={i} style={{ marginBottom: 8 }}>
          <strong>{d.feature}</strong> — {d.level}
          <div style={{ fontSize: "0.9em", color: "#555" }}>
            {d.explanation} (Δ {d.driftScore.toFixed(2)})
          </div>
        </div>
      ))}
    </div>
  );
};
