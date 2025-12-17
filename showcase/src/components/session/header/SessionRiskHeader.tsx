
import { MlConfidenceBadge } from "../ml/MlConfidenceBadge";

export function SessionRiskHeader({ risk, decision, confidence }: any) {
  return (
    <div style={{
      position: "sticky",
      top: 0,
      zIndex: 10,
      background: "#111",
      padding: "12px",
      borderBottom: "1px solid #333"
    }}>
      <strong>Risk:</strong> {risk} |
      <strong> Decision:</strong> {decision} |
      <MlConfidenceBadge confidence={confidence} />
      <div style={{ fontSize: "0.85em", opacity: 0.8 }}>
        Compared against your usual behaviour and device
      </div>
    </div>
  );
}
