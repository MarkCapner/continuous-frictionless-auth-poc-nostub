import React, { useEffect, useState } from "react";
import type { TlsFingerprintSummary } from "../api";
import { fetchAllTlsFingerprintSummaries } from "../api";
import { TlsFingerprintInspector } from "./TlsFingerprintInspector";
import { ExpandablePanel } from "../ui/ExpandablePanel";

export function AdminTlsView() {
  const [summaries, setSummaries] = useState<TlsFingerprintSummary[]>([]);
  const [selectedFp, setSelectedFp] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    const run = async () => {
      setLoading(true);
      setError(null);
      try {
        const data = await fetchAllTlsFingerprintSummaries(200);
        if (!cancelled) {
          setSummaries(data);
          if (!selectedFp && data.length > 0) {
            setSelectedFp(data[0].tlsFp);
          }
        }
      } catch (e: any) {
        if (!cancelled) {
          setError(e.message ?? String(e));
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    };

    void run();
    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <div className="stack">
      <div className="pageHeader">
        <div>
          <h2>Admin / TLS fingerprints</h2>
          <p>
            Explore all known TLS fingerprints and drill into each one. Select a row to view the
            family, variants, and normalisation output.
          </p>
        </div>
        <div />
      </div>

      <div className="grid2">
        <div className="card">
          <div className="cardTitle">
            <h3>Fingerprints</h3>
            <span className="muted">{summaries.length} total</span>
          </div>

          {loading && <p className="muted">Loading TLS fingerprints…</p>}
          {error && <p className="muted" style={{ color: "var(--danger)" }}>{error}</p>}
          {!loading && !error && summaries.length === 0 && (
            <p className="muted">No fingerprints yet. Run some profile checks first.</p>
          )}

          {!loading && !error && summaries.length > 0 && (
            <div style={{ maxHeight: 420, overflow: "auto", display: "flex", flexDirection: "column", gap: 10 }}>
              {summaries.map((s) => {
                const active = s.tlsFp === selectedFp;
                return (
                  <div
                    key={s.tlsFp}
                    className={`summaryCard rowBtn ${active ? "rowActive" : ""}`}
                    onClick={() => setSelectedFp(s.tlsFp)}
                    style={{ cursor: "pointer" }}
                  >
                    <div style={{ display: "flex", justifyContent: "space-between", gap: 10, alignItems: "center" }}>
                      <div className="mono" style={{ fontWeight: 750, wordBreak: "break-all" }}>{s.tlsFp}</div>
                      <div style={{ display: "flex", gap: 6, flexWrap: "wrap", justifyContent: "flex-end" }}>
                        <span className="chip">profiles · {s.profiles}</span>
                        <span className="chip">users · {s.users}</span>
                      </div>
                    </div>
                    <ExpandablePanel
                      title="TLS fingerprint details"
                      hint="First/last observed"
                    >
                      <div className="stack" style={{ gap: 6 }}>
                        <div style={{ display: "flex", justifyContent: "space-between", gap: 12 }}>
                          <span className="muted">First seen</span>
                          <span className="muted">{new Date(s.firstSeen).toLocaleString()}</span>
                        </div>
                        <div style={{ display: "flex", justifyContent: "space-between", gap: 12 }}>
                          <span className="muted">Last seen</span>
                          <span className="muted">{new Date(s.lastSeen).toLocaleString()}</span>
                        </div>
                      </div>
                    </ExpandablePanel>
                  </div>
                );
              })}
            </div>
          )}
        </div>

        <div className="stack">
          <TlsFingerprintInspector tlsFp={selectedFp} />
        </div>
      </div>
    </div>
  );
}
