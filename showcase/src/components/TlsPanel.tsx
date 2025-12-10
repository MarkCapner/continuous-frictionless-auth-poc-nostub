import type { DecisionResponse } from "../api";

interface TlsPanelProps {
  decision: DecisionResponse | null;
}

export function TlsPanel({ decision }: TlsPanelProps) {
  return (
    <div style={cardStyle}>
      <h2>TLS Fingerprint</h2>
      {!decision && <p>No decision yet. TLS FP will appear after the first profile check.</p>}
      {decision && (
        <>
          <p>
            <strong>TLS FP (ja4):</strong>{" "}
            <code>{decision.tls_fp ?? "not provided"}</code>
          </p>
          <p>
            <strong>TLS meta:</strong>{" "}
            <code>{decision.tls_meta ?? "not provided"}</code>
          </p>
          <p style={{ fontSize: "0.8rem", color: "#666" }}>
            These values are computed at the gateway from the ClientHello and forwarded to the risk API.
          </p>
        </>
      )}
    </div>
  );
}

const cardStyle = {
  border: "1px solid #ddd",
  borderRadius: 8,
  padding: "1rem",
  background: "#fff",
  boxShadow: "0 1px 3px rgba(0,0,0,0.05)",
  flex: 1,
  minWidth: 260
};
