import React, { useEffect, useState } from "react";
import type { BehaviorBaseline } from "../api";
import { fetchBehaviorBaselines } from "../api";

export function AdminBehaviorView() {
  const [rows, setRows] = useState<BehaviorBaseline[]>([]);
  const [selected, setSelected] = useState<BehaviorBaseline | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    const run = async () => {
      setLoading(true);
      setError(null);
      try {
        const data = await fetchBehaviorBaselines(500);
        if (!cancelled) {
          setRows(data);
          if (!selected && data.length > 0) setSelected(data[0]);
        }
      } catch (e: any) {
        if (!cancelled) setError(e.message ?? String(e));
      } finally {
        if (!cancelled) setLoading(false);
      }
    };

    void run();
    return () => { cancelled = true; };
  }, []);

  return (
    <div className="stack">
      <div className="pageHeader">
        <div>
          <h2>Admin / Behaviour baselines</h2>
          <p>
            Per-user behavioural baselines derived from keystroke timing, scroll cadence and pointer dynamics.
            Each row comes from <span className="mono">behavior_profile_stats</span>.
          </p>
        </div>
        <div />
      </div>

      <div className="grid2">
        <div className="card">
          <div className="cardTitle">
            <h3>Baselines</h3>
            <span className="muted">{rows.length} rows</span>
          </div>

          {loading && <p className="muted">Loading baselines…</p>}
          {error && <p className="muted" style={{ color: "var(--danger)" }}>Failed to load baselines: {error}</p>}
          {!loading && !error && rows.length === 0 && (
            <p className="muted">
              No behavioural baselines yet. Generate some sessions via the Showcase view first.
            </p>
          )}

          {!loading && !error && rows.length > 0 && (
            <div className="tableWrap" style={{ maxHeight: 460 }}>
              <table className="table">
                <thead>
                  <tr>
                    <th>User</th>
                    <th>Feature</th>
                    <th>Mean</th>
                    <th>Std dev</th>
                    <th>Decay</th>
                    <th>Updated</th>
                  </tr>
                </thead>
                <tbody>
                  {rows.map((r, idx) => {
                    const active = selected && r.userId === selected.userId && r.feature === selected.feature;
                    return (
                      <tr
                        key={`${r.userId}-${r.feature}-${idx}`}
                        onClick={() => setSelected(r)}
                        className={`rowBtn ${active ? "rowActive" : ""}`}
                      >
                        <td className="mono">{r.userId}</td>
                        <td className="mono">{r.feature}</td>
                        <td>{r.mean.toFixed(4)}</td>
                        <td>{r.stdDev.toFixed(4)}</td>
                        <td>{r.decay.toFixed(4)}</td>
                        <td className="muted">{new Date(r.updatedAt).toLocaleString()}</td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </div>

        <div className="card">
          <div className="cardTitle">
            <h3>Baseline detail</h3>
            <span className="muted">{selected ? <span className="mono">{selected.userId}</span> : "—"}</span>
          </div>

          {!selected && <p className="muted">Select a baseline row to view details.</p>}
          {selected && (
            <div className="stack" style={{ gap: 10 }}>
              <div className="grid2Tight">
                <div className="card cardFlat">
                  <div className="muted">Feature</div>
                  <div className="mono">{selected.feature}</div>
                </div>
                <div className="card cardFlat">
                  <div className="muted">Updated</div>
                  <div className="mono">{new Date(selected.updatedAt).toLocaleString()}</div>
                </div>
              </div>

              <div className="card cardFlat">
                <div className="muted" style={{ marginBottom: 8 }}>Stats</div>
                <div style={{ display: "flex", justifyContent: "space-between", gap: 12 }}>
                  <span className="muted">Mean</span>
                  <span className="mono">{selected.mean.toFixed(6)}</span>
                </div>
                <div style={{ display: "flex", justifyContent: "space-between", gap: 12 }}>
                  <span className="muted">Std dev</span>
                  <span className="mono">{selected.stdDev.toFixed(6)}</span>
                </div>
                <div style={{ display: "flex", justifyContent: "space-between", gap: 12 }}>
                  <span className="muted">Decay</span>
                  <span className="mono">{selected.decay.toFixed(6)}</span>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
