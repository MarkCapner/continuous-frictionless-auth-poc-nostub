import { useEffect, useMemo, useState } from "react";
import type { TrustSnapshot, TrustSignal, TrustDiffItem } from "../api";
import { getTrustSnapshot } from "../api";

function statusChip(status: string): { label: string; className: string } {
  const s = (status || "").toUpperCase();
  if (s === "OK") return { label: "OK", className: "chip chipAccent" };
  if (s === "WARN") return { label: "CAUTION", className: "chip chipAccent2" };
  if (s === "RISK") return { label: "RISK", className: "chip chipDanger" };
  return { label: s || "INFO", className: "chip" };
}

function severityLabel(sev: string): string {
  const s = (sev || "").toUpperCase();
  if (s === "HIGH") return "High";
  if (s === "MEDIUM") return "Medium";
  return "Low";
}

export function TrustSnapshotPanel(props: { sessionId?: string; compact?: boolean }) {
  const { sessionId, compact } = props;

  const [snapshot, setSnapshot] = useState<TrustSnapshot | null>(null);
  const [err, setErr] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const canLoad = useMemo(() => !!sessionId && sessionId.trim().length > 0, [sessionId]);

  useEffect(() => {
    let cancelled = false;
    async function run() {
      if (!canLoad || !sessionId) return;
      setLoading(true);
      setErr(null);
      try {
        const data = await getTrustSnapshot(sessionId);
        if (!cancelled) setSnapshot(data);
      } catch (e: any) {
        if (!cancelled) setErr(e?.message ?? "Failed to load trust snapshot");
      } finally {
        if (!cancelled) setLoading(false);
      }
    }
    run();
    return () => {
      cancelled = true;
    };
  }, [canLoad, sessionId]);

  if (!canLoad) {
    return (
      <div className="card">
        <div className="cardTitle">
          <h3>Trust snapshot</h3>
          <span className="chip">no session</span>
        </div>
        <p className="muted" style={{ margin: 0 }}>
          Run a profile check to generate a session, then a user-friendly trust explanation will appear here.
        </p>
      </div>
    );
  }

  if (loading && !snapshot) {
    return (
      <div className="card">
        <div className="cardTitle">
          <h3>Trust snapshot</h3>
          <span className="chip">loading</span>
        </div>
        <p className="muted" style={{ margin: 0 }}>
          Building a user-friendly explanation…
        </p>
      </div>
    );
  }

  if (err) {
    return (
      <div className="card">
        <div className="cardTitle">
          <h3>Trust snapshot</h3>
          <span className="chip chipDanger">error</span>
        </div>
        <p className="muted" style={{ margin: 0 }}>
          {err}
        </p>
      </div>
    );
  }

  if (!snapshot) return null;

  return (
    <div className="card">
      <div className="cardTitle">
        <h3>Trust snapshot</h3>
        <span className="chip">{snapshot.decision}</span>
      </div>

      <div style={{ marginTop: 8 }}>
        <div className="muted" style={{ fontSize: 12, marginBottom: 6 }}>In plain language</div>
        <div style={{ fontSize: 14, lineHeight: 1.45 }}>{snapshot.riskSummary}</div>
      </div>

      <div style={{ marginTop: 14 }}>
        <div className="muted" style={{ fontSize: 12, marginBottom: 6 }}>Signals</div>
        <div style={{ display: "flex", flexDirection: "column", gap: 10 }}>
          {snapshot.signals.map((s: TrustSignal, idx: number) => {
            const chip = statusChip(s.status);
            return (
              <div key={idx} style={{ display: "flex", gap: 10, alignItems: "flex-start" }}>
                <span className={chip.className} style={{ minWidth: 74, textAlign: "center" }}>{chip.label}</span>
                <div style={{ minWidth: 0 }}>
                  <div style={{ fontWeight: 600 }}>{s.label}</div>
                  <div className="muted" style={{ fontSize: 12, marginTop: 2, lineHeight: 1.35 }}>
                    {s.explanation}
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {!compact ? (
        <div style={{ marginTop: 16 }}>
          <div className="muted" style={{ fontSize: 12, marginBottom: 6 }}>
            What changed since last time
            {snapshot.baselineSessionId ? (
              <span className="mono" style={{ marginLeft: 8, opacity: 0.8 }}>
                baseline: {snapshot.baselineSessionId}
              </span>
            ) : null}
          </div>

          {snapshot.changes && snapshot.changes.length > 0 ? (
            <ul style={{ margin: 0, paddingLeft: 18 }}>
              {snapshot.changes.map((c: TrustDiffItem, idx: number) => (
                <li key={idx} style={{ marginBottom: 6 }}>
                  <span style={{ fontWeight: 600 }}>{c.dimension}</span>: {c.change}{" "}
                  <span className="muted" style={{ fontSize: 12 }}>({severityLabel(c.severity)})</span>
                </li>
              ))}
            </ul>
          ) : (
            <p className="muted" style={{ margin: 0 }}>
              Nothing meaningful changed compared to your last trusted session.
            </p>
          )}
        </div>
      ) : null}
    </div>
  );
}
