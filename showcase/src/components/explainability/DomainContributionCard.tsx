
import React from "react";

export type DeviationLabel = "NORMAL" | "ELEVATED" | "EXTREME";

export function DomainContributionCard({ domain, score, label }:{
  domain: string; score: number; label: DeviationLabel;
}) {
  return (
    <div className={`card deviation-${label.toLowerCase()}`}>
      <h3>{domain}</h3>
      <strong>{score.toFixed(3)}</strong>
      <div>{label}</div>
    </div>
  );
}
