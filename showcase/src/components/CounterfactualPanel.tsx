import React from "react";

type Counterfactual = {
  feature: string;
  deltaRequired: number;
  explanation: string;
};

type Props = {
  counterfactuals?: Counterfactual[];
};

export const CounterfactualPanel: React.FC<Props> = ({ counterfactuals = [] }) => {
  if (counterfactuals.length === 0) {
    return <em>No counterfactual suggestions available.</em>;
  }

  return (
    <div style={{ border: "1px dashed #ccc", borderRadius: 8, padding: 16, marginTop: 16 }}>
      <h3>What would change this decision?</h3>
      <ul>
        {counterfactuals.map((c, i) => (
          <li key={i}>
            <strong>{c.feature}</strong>: {c.explanation}
          </li>
        ))}
      </ul>
    </div>
  );
};
