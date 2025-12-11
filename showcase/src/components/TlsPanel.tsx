import type { DecisionResponse } from "../api";

interface TlsPanelProps {
  decision: DecisionResponse | null;
}

export function TlsPanel({ decision }: TlsPanelProps) {
  const suspiciousReuse =
    !!decision &&
    Array.isArray(decision.explanations) &&
    decision.explanations.some((e) => e.includes("TLS fingerprint reused across"));

  return (
    <div style={cardStyle}>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "baseline" }}>
        <h2 style={{ margin: 0 }}>TLS Fingerprint</h2>
        {suspiciousReuse && (
          <span style={badgeStyle} title="This TLS fingerprint is heavily reused across users/devices">
            Suspicious reuse
          </span>
        )}
      </div>
      {!decision && <p style={{ marginTop: "0.5rem" }}>No decision yet. TLS FP will appear after the first profile check.</p>}
      {decision && (
        <>
          <p style={{ marginTop: "0.5rem" }}>
            <strong>TLS FP (ja4-style hash):</strong>{" "}
            <code>{decision.tls_fp ?? "not provided"}</code>
          </p>
          {decision.tls_meta && (
            <p style={{ fontSize: "0.8rem", color: "#4b5563" }}>
              <strong>Meta:</strong> {decision.tls_meta}
            </p>
          )}
          {decision.explanations && decision.explanations.length > 0 && (
            <ul style={{ fontSize: "0.8rem", paddingLeft: "1.1rem", marginTop: "0.5rem" }}>
              {decision.explanations.map((e, idx) => (
                <li key={idx}>{e}</li>
              ))}
            </ul>
          )}
        </>
      )}
    </div>
  );
}

const cardStyle: React.CSSProperties = {
  border: "1px solid #ddd",
  borderRadius: 8,
  padding: "1rem",
  background: "#fff",
  boxShadow: "0 1px 3px rgba(0,0,0,0.05)",
  flex: 1,
  minWidth: 260
};

const badgeStyle = {
  fontSize: "0.7rem",
  textTransform: "uppercase",
  letterSpacing: 0.5,
  padding: "0.15rem 0.4rem",
  borderRadius: 999,
  border: "1px solid #b91c1c",
  color: "#b91c1c",
  background: "#fef2f2"
};
