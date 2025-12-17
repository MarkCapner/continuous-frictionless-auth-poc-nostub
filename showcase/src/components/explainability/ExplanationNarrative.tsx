
import React from "react";

export function ExplanationNarrative({ primaryDomain, label }:{
  primaryDomain: string; label: string;
}) {
  return (
    <p>
      This session was primarily influenced by <b>{primaryDomain}</b> signals,
      which were classified as <b>{label}</b> compared to historical baselines.
    </p>
  );
}
