import { useEffect, useState } from "react";
import { startProfiler, snapshotTelemetry } from "./profiler";
import type { TelemetryPayload, DecisionResponse } from "./api";
import { postProfileCheck } from "./api";

function App() {
  const [telemetry, setTelemetry] = useState<TelemetryPayload | null>(null);
  const [decision, setDecision] = useState<DecisionResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [optOut, setOptOut] = useState(false);

  useEffect(() => {
    startProfiler();
  }, []);

  const runProfileCheck = async () => {
    setError(null);
    setLoading(true);
    try {
      const payload = snapshotTelemetry("demo-user", optOut);
      setTelemetry(payload);
      const resp = await postProfileCheck(payload);
      setDecision(resp);
    } catch (e: any) {
      setError(e.message ?? String(e));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ padding: "1rem", fontFamily: "system-ui", maxWidth: 900, margin: "0 auto" }}>
      <h1>Continuous Frictionless Auth – Showcase</h1>
      <p>
        This page captures device and behavior telemetry <strong>in memory only</strong> (no cookies or localStorage)
        and sends it to the Risk API for ML + rules-based scoring.
      </p>

      <label style={{ display: "block", margin: "0.75rem 0" }}>
        <input
          type="checkbox"
          checked={optOut}
          onChange={(e) => setOptOut(e.target.checked)}
        />{" "}
        Opt out of profiling (no data stored)
      </label>

      <button onClick={runProfileCheck} disabled={loading}>
        {loading ? "Running..." : "Run profile check"}
      </button>

      {error && (
        <p style={{ color: "red", marginTop: "0.5rem" }}>
          Error: {error}
        </p>
      )}

      {telemetry && (
        <div style={{ marginTop: "1rem" }}>
          <h2>Telemetry snapshot</h2>
          <pre style={{ background: "#f6f6f6", padding: "0.75rem", borderRadius: 8 }}>
            {JSON.stringify(telemetry, null, 2)}
          </pre>
        </div>
      )}

      {decision && (
        <div style={{ marginTop: "1rem" }}>
          <h2>Decision</h2>
          <p>
            Decision: <strong>{decision.decision}</strong>{" "}
            (confidence: {(decision.confidence * 100).toFixed(1)}%)
          </p>
          <h3>Breakdown</h3>
          <pre style={{ background: "#f6f6f6", padding: "0.75rem", borderRadius: 8 }}>
            {JSON.stringify(decision.breakdown, null, 2)}
          </pre>
          <h3>Explanations</h3>
          <ul>
            {decision.explanations.map((e, idx) => (
              <li key={idx}>{e}</li>
            ))}
          </ul>
          <p>Session ID: {decision.session_id}</p>
        </div>
      )}
    </div>
  );
}

export default App;
