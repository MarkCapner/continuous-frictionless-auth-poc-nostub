import { useEffect, useMemo, useState } from "react";
import type { TrustSignal, TrustSnapshot } from "../api";
import { getTrustSnapshot } from "../api";
import { useSessionContext } from "../state/session";
import { ExpandablePanel } from "./ExpandablePanel";
import { JsonOptIn } from "./JsonOptIn";

function decisionChip(decision: string) {
  const d = (decision || "").toUpperCase();
  if (d === "ALLOW" || d === "TRUST" || d === "PASS" || d === "AUTO_LOGIN") return { label: d || "TRUSTED", className: "chip chipAccent" };
  if (d.includes("STEP")) return { label: d, className: "chip chipAccent2" };
  if (d === "BLOCK" || d === "DENY") return { label: d, className: "chip chipDanger" };
  return { label: d || "UNKNOWN", className: "chip" };
}

function signalIcon(s: TrustSignal): string {
  const cat = (s.category || "").toUpperCase();
  const status = (s.status || "").toUpperCase();
  const bang = status === "WARN" || status === "RISK" ? "⚠️" : "";
  if (cat === "DEVICE") return `🖥️${bang}`;
  if (cat === "BEHAVIOUR" || cat === "BEHAVIOR") return `⌨️${bang}`;
  if (cat === "TLS") return `🔒${bang}`;
  if (cat === "CONTEXT") return `🌐${bang}`;
  return `ℹ️${bang}`;
}

/**
 * Sticky, cross-view risk summary header.
 * - Pulls TrustSnapshot for the globally selected session.
 * - Raw JSON remains opt-in.
 */
export function StickyRiskHeader() {
  const { selectedSessionId } = useSessionContext();
  const [snapshot, setSnapshot] = useState<TrustSnapshot | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    const sid = (selectedSessionId || "").trim();
    if (!sid) {
      setSnapshot(null);
      setError(null);
      return;
    }

    const run = async () => {
      setLoading(true);
      setError(null);
      try {
        const s = await getTrustSnapshot(sid);
        if (!cancelled) setSnapshot(s);
      } catch (e: any) {
        if (!cancelled) {
          setSnapshot(null);
          setError(e?.message ?? String(e));
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    };
    void run();
    return () => {
      cancelled = true;
    };
  }, [selectedSessionId]);

  const header = useMemo(() => {
    if (!selectedSessionId) return { title: "No session selected", subtitle: "Pick a session from the global selector" };
    if (loading) return { title: `Session ${selectedSessionId.slice(0, 8)}…`, subtitle: "Loading trust snapshot…" };
    if (error) return { title: `Session ${selectedSessionId.slice(0, 8)}…`, subtitle: `Snapshot unavailable: ${error}` };
    if (!snapshot) return { title: `Session ${selectedSessionId.slice(0, 8)}…`, subtitle: "No snapshot available" };
    return { title: `Session ${snapshot.sessionId.slice(0, 8)}…`, subtitle: snapshot.riskSummary };
  }, [selectedSessionId, loading, error, snapshot]);

  if (!selectedSessionId) {
    return (
      <div className="riskStickyBar">
        <div style={{ display: "flex", flexDirection: "column", gap: 4 }}>
          <div style={{ fontWeight: 700 }}>{header.title}</div>
          <div className="muted" style={{ fontSize: 12 }}>{header.subtitle}</div>
        </div>
      </div>
    );
  }

  const chip = decisionChip(snapshot?.decision ?? "");

  return (
    <div className="riskStickyBar" role="region" aria-label="Selected session risk summary">
      <div className="riskStickyRow">
        <div style={{ display: "flex", flexDirection: "column", gap: 4, minWidth: 260 }}>
          <div style={{ display: "flex", alignItems: "center", gap: 10, flexWrap: "wrap" }}>
            <div style={{ fontWeight: 800 }}>{header.title}</div>
            {snapshot ? (
              <>
                <span className={chip.className}>{chip.label}</span>
                <span className="chip">confidence {(snapshot.confidence * 100).toFixed(1)}%</span>
              </>
            ) : null}
          </div>
          <div className="muted" style={{ fontSize: 12, maxWidth: 880, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>
            {header.subtitle}
          </div>
        </div>

        <div style={{ flex: 1 }} />

        {snapshot && snapshot.signals && snapshot.signals.length > 0 ? (
          <div style={{ display: "flex", gap: 8, flexWrap: "wrap", justifyContent: "flex-end" }}>
            {snapshot.signals.slice(0, 8).map((sig, idx) => (
              <span key={idx} className="chip" title={sig.explanation || sig.label}>
                {signalIcon(sig)} {sig.label}
              </span>
            ))}
            {snapshot.signals.length > 8 ? <span className="chip">+{snapshot.signals.length - 8} more</span> : null}
          </div>
        ) : null}
      </div>

      {snapshot ? (
        <div style={{ marginTop: 10 }}>
          <ExpandablePanel title="Selected session details" hint="Raw JSON is opt-in" defaultOpen={false}>
            <div className="grid2Equal">
              <div className="card" style={{ background: "rgba(0,0,0,0.18)" }}>
                <div className="muted" style={{ fontSize: 12, marginBottom: 8 }}>Signals</div>
                <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
                  {(snapshot.signals || []).map((sig, idx) => (
                    <div key={idx} style={{ display: "flex", gap: 10, alignItems: "baseline" }}>
                      <span className="chip">{signalIcon(sig)} {sig.status}</span>
                      <div>
                        <div style={{ fontWeight: 700 }}>{sig.label}</div>
                        <div className="muted" style={{ fontSize: 12 }}>{sig.explanation}</div>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
              <div className="card" style={{ background: "rgba(0,0,0,0.18)" }}>
                <div className="muted" style={{ fontSize: 12, marginBottom: 8 }}>Snapshot</div>
                <div style={{ display: "flex", flexWrap: "wrap", gap: 10 }}>
                  <span className="chip">session {snapshot.sessionId}</span>
                  {snapshot.userId ? <span className="chip">user {snapshot.userId}</span> : null}
                  {snapshot.baselineSessionId ? <span className="chip">baseline {snapshot.baselineSessionId.slice(0, 8)}…</span> : null}
                  {snapshot.consentGranted === false ? <span className="chip chipDanger">consent not granted</span> : null}
                </div>
                <div style={{ height: 10 }} />
                <JsonOptIn title="Trust snapshot JSON" value={snapshot} />
              </div>
            </div>
          </ExpandablePanel>
        </div>
      ) : null}
    </div>
  );
}
