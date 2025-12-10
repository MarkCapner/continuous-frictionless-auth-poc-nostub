import { useEffect, useState } from "react";
import { startProfiler, snapshotTelemetry } from "./profiler";
import type { TelemetryPayload, DecisionResponse } from "./api";
import { postProfileCheck } from "./api";
import { DeviceCard } from "./components/DeviceCard";
import { TlsPanel } from "./components/TlsPanel";
import { BehaviorPanel } from "./components/BehaviorPanel";
import { SessionTimeline } from "./components/SessionTimeline";
import { UsersOverview } from "./components/UsersOverview";
import { ChaosToggles } from "./components/ChaosToggles";

function App() {
  const [telemetry, setTelemetry] = useState<TelemetryPayload | null>(null);
  const [decision, setDecision] = useState<DecisionResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [demoUser, setDemoUser] = useState<string>("demo-user");

  useEffect(() => {
    startProfiler();
  }, []);

  const runProfileCheck = async () => {
    setError(null);
    setLoading(true);
    try {
      const userHint = demoUser.trim() || "demo-user";
      const payload = snapshotTelemetry(userHint);
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
    <div style={pageStyle}>
      <header>
        <h1>Continuous Frictionless Auth – Showcase</h1>
        <p style={{ maxWidth: 640 }}>
          This page visualises the device profile, TLS fingerprint, lightweight behavioural biometrics and
          risk decisions used by the PoC. No cookies or local storage are used; everything is computed in-memory.
        </p>
      </header>

            <div style={userRowStyle}>
        <label style={{ fontWeight: 500 }}>
          Demo user handle:
          <input
            type="text"
            value={demoUser}
            onChange={e => setDemoUser(e.target.value)}
            placeholder="e.g. mark-demo, alice-laptop"
            style={inputStyle}
          />
        </label>
        <span style={{ fontSize: "0.85rem", color: "#4b5563" }}>
          This handle is sent as <code>user_id_hint</code> and used to group sessions and devices.
        </span>
      </div>


      <div style={{ marginBottom: "1rem" }}>
        <button onClick={runProfileCheck} disabled={loading} style={buttonStyle}>
          {loading ? "Running profile check..." : "Run profile check"}
        </button>
        {error && <span style={{ color: "red", marginLeft: "1rem" }}>{error}</span>}
      </div>

      <section style={gridTwoCols}>
        <DeviceCard device={telemetry ? telemetry.device : null} />
        <TlsPanel decision={decision} />
      </section>

      <section style={gridTwoCols}>
        <BehaviorPanel behavior={telemetry ? telemetry.behavior : null} />
        <ChaosToggles />
      </section>

      <section style={gridTwoCols}>
        <SessionTimeline userHint={demoUser.trim() || "demo-user"} />
        <UsersOverview />
      </section>

      {decision && (
        <section style={{ marginTop: "1.5rem" }}>
          <h2>Decision details</h2>
          <p>
            Decision: <strong>{decision.decision}</strong>{" "}
            (confidence: {(decision.confidence * 100).toFixed(1)}%)
          </p>
          <h3>Score breakdown</h3>
          <pre style={preStyle}>{JSON.stringify(decision.breakdown, null, 2)}</pre>
          <h3>Explanations</h3>
          <ul>
            {decision.explanations.map((e, idx) => (
              <li key={idx}>{e}</li>
            ))}
          </ul>
          <p>Session ID: {decision.session_id}</p>
        </section>
      )}
    </div>
  );
}

const pageStyle = {
  fontFamily: "system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif",
  padding: "1.5rem",
  background: "#f3f4f6",
  minHeight: "100vh",
  boxSizing: "border-box"
};

const gridTwoCols = {
  display: "flex",
  flexWrap: "wrap",
  gap: "1rem",
  alignItems: "stretch",
  marginTop: "1rem"
};

const userRowStyle = {
  marginTop: "1rem",
  marginBottom: "0.75rem",
  display: "flex",
  flexDirection: "column",
  gap: "0.25rem",
  maxWidth: 520
};

const inputStyle = {
  display: "block",
  marginTop: "0.25rem",
  padding: "0.35rem 0.5rem",
  borderRadius: 6,
  border: "1px solid #d1d5db",
  fontSize: "0.9rem",
  width: "100%",
  maxWidth: 260
};

const buttonStyle = {
  padding: "0.5rem 1rem",
  borderRadius: 999,
  border: "none",
  background: "#4f46e5",
  color: "#fff",
  cursor: "pointer",
  fontSize: "0.95rem"
};

const preStyle = {
  background: "#111827",
  color: "#e5e7eb",
  padding: "0.75rem",
  borderRadius: 8,
  fontSize: "0.8rem",
  overflowX: "auto"
};

export default App;
