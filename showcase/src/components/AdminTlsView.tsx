import React, { useEffect, useState } from "react";
import type { TlsFingerprintSummary } from "../api";
import { fetchAllTlsFingerprintSummaries } from "../api";
import { TlsFingerprintInspector } from "./TlsFingerprintInspector";

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
            <div className="tableWrap" style={{ maxHeight: 420 }}>
              <table className="table">
                <thead>
                  <tr>
                    <th>TLS FP</th>
                    <th>Profiles</th>
                    <th>Users</th>
                    <th>First seen</th>
                    <th>Last seen</th>
                  </tr>
                </thead>
                <tbody>
                  {summaries.map((s) => {
                    const active = s.tlsFp === selectedFp;
                    return (
                      <tr
                        key={s.tlsFp}
                        onClick={() => setSelectedFp(s.tlsFp)}
                        className={`rowBtn ${active ? "rowActive" : ""}`}
                      >
                        <td className="mono">{s.tlsFp}</td>
                        <td>{s.profiles}</td>
                        <td>{s.users}</td>
                        <td className="muted">{new Date(s.firstSeen).toLocaleString()}</td>
                        <td className="muted">{new Date(s.lastSeen).toLocaleString()}</td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
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
