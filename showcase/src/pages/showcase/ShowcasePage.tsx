import { useEffect, useState } from "react";
import type { DecisionResponse, TelemetryPayload } from "../../api";
import { postProfileCheck } from "../../api";
import { snapshotTelemetry, startProfiler } from "../../profiler";
import { BehaviorPanel } from "../../components/BehaviorPanel";
import { ChaosToggles } from "../../components/ChaosToggles";
import { DeviceCard } from "../../components/DeviceCard";
import { SessionTimeline } from "../../components/SessionTimeline";
import { TlsFingerprintInspector } from "../../components/TlsFingerprintInspector";
import { TlsPanel } from "../../components/TlsPanel";
import { TrustSnapshotPanel } from "../../components/TrustSnapshotPanel";
import { UsersOverview } from "../../components/UsersOverview";
import { JsonOptIn } from "../../ui/JsonOptIn";
import { ExpandablePanel } from "../../ui/ExpandablePanel";

export function ShowcasePage(props: {
  onDecisionChanged?: (decision: DecisionResponse | null) => void;
}) {
  const { onDecisionChanged } = props;

  const [telemetry, setTelemetry] = useState<TelemetryPayload | null>(null);
  const [decision, setDecision] = useState<DecisionResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [demoUser, setDemoUser] = useState<string>("demo-user");
  // EPIC 13.6: optional tenant id used for per-tenant policy overrides.
  const [demoTenant, setDemoTenant] = useState<string>("demo-tenant");

  useEffect(() => {
    startProfiler();
  }, []);

  useEffect(() => {
    onDecisionChanged?.(decision);
  }, [decision, onDecisionChanged]);

  const runProfileCheck = async () => {
    setError(null);
    setLoading(true);
    try {
      const userHint = demoUser.trim() || "demo-user";
      const tenantHint = demoTenant.trim() || undefined;
      const payload = snapshotTelemetry(userHint, tenantHint);
      setTelemetry(payload);
      const resp = await postProfileCheck(payload);
      setDecision(resp);
    } catch (e: any) {
      setError(e?.message ?? String(e));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 14 }}>
      <div className="card">
        <div className="cardTitle">
          <h3>Session controls</h3>
          <div style={{ display: "flex", gap: 10, alignItems: "center" }}>
            {error ? <span className="chip chipDanger">{error}</span> : null}
          </div>
        </div>

        <div className="grid2Equal">
          <label style={{ display: "block" }}>
            <span className="muted">Demo user handle</span>
            <input
              className="input"
              type="text"
              value={demoUser}
              onChange={(e) => setDemoUser(e.target.value)}
              placeholder="e.g. mark-demo, alice-laptop"
            />
            <div className="muted" style={{ fontSize: 12, marginTop: 6 }}>
              Sent as <span className="mono">user_id_hint</span> so you can compare devices per handle.
            </div>

            <div style={{ height: 10 }} />

            <span className="muted">Tenant (optional)</span>
            <input
              className="input"
              type="text"
              value={demoTenant}
              onChange={(e) => setDemoTenant(e.target.value)}
              placeholder="e.g. demo-tenant, corp-uk"
            />
            <div className="muted" style={{ fontSize: 12, marginTop: 6 }}>
              Sent as <span className="mono">context.tenant_id</span> to enable per-tenant policy overrides.
            </div>
          </label>

          <div>
            <div className="muted" style={{ fontSize: 12, marginBottom: 8 }}>
              Controls
            </div>
            <div style={{ display: "flex", flexWrap: "wrap", gap: 10 }}>
              <button className="btn btnPrimary" onClick={runProfileCheck} disabled={loading}>
                {loading ? "Running…" : "Run profile check"}
              </button>
              <span className="chip chipAccent">No cookies</span>
              <span className="chip chipAccent2">No local storage</span>
            </div>
          </div>
        </div>
      </div>

      <div className="grid2">
        <DeviceCard device={telemetry ? telemetry.device : null} />
        <div style={{ display: "flex", flexDirection: "column", gap: 14 }}>
          <TlsPanel decision={decision} />
          <TlsFingerprintInspector tlsFp={decision?.tls_fp} />
        </div>
      </div>

      <div className="grid2">
        <BehaviorPanel behavior={telemetry ? telemetry.behavior : null} />
        <ChaosToggles />
      </div>

      <div className="grid2">
        <SessionTimeline userHint={demoUser.trim() || "demo-user"} />
        <UsersOverview />
      </div>

      {decision ? (
        <>
          <div className="card">
            <div className="cardTitle">
              <h3>Decision details</h3>
              <span className="chip chipAccent">
                {decision.decision} · {(decision.confidence * 100).toFixed(1)}%
              </span>
            </div>

            <div className="muted" style={{ fontSize: 12, marginBottom: 10 }}>
              Model: <span className="mono">{decision.model_version ?? "synthetic / unknown"}</span>
            </div>

            {typeof decision.breakdown.ml_anomaly_score === "number" ? (
              <div className="muted" style={{ fontSize: 12, marginBottom: 10 }}>
                Anomaly score: <span className="mono">{decision.breakdown.ml_anomaly_score!.toFixed(3)}</span> (0 = normal,
                1 = very unusual)
              </div>
            ) : null}

            <div className="divider" />

            <div className="stack" style={{ gap: 12 }}>
              <ExpandablePanel
                title="Decision explainability"
                hint="Breakdown & signals are available, but raw JSON is opt-in"
                defaultOpen
              >
                <div className="grid2Equal">
                  <JsonOptIn title="Score breakdown" value={decision.breakdown} />
                  <JsonOptIn title="Signals" value={decision.signals} />
                </div>
              </ExpandablePanel>
            </div>
          </div>

          <TrustSnapshotPanel userHint={demoUser.trim() || "demo-user"} />
        </>
      ) : null}
    </div>
  );
}
