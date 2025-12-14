import { useEffect, useState } from "react";
import type { ModelInfo } from "../api";
import { fetchModelInfo, retrainModel } from "../api";

export function AdminMlView() {
  const [info, setInfo] = useState<ModelInfo | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [limit, setLimit] = useState(500);

  async function loadInfo() {
    try {
      setError(null);
      const data = await fetchModelInfo();
      setInfo(data);
    } catch (e: any) {
      setError(e?.message ?? String(e));
    }
  }

  useEffect(() => { void loadInfo(); }, []);

  async function handleRetrain() {
    try {
      setLoading(true);
      setError(null);
      const data = await retrainModel(limit);
      setInfo(data);
    } catch (e: any) {
      setError(e?.message ?? String(e));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="stack">
      <div className="pageHeader">
        <div>
          <h2>Admin / ML model</h2>
          <p>
            Shows the active model in <span className="mono">model_registry</span> and lets you trigger
            a retrain from recent <span className="mono">session_feature</span> rows.
          </p>
        </div>
        <div />
      </div>

      {error && <div className="card cardDanger">
        <div className="muted textDanger">Error: {error}</div>
      </div>}

      <div className="grid2">
        <div className="card">
          <div className="cardTitle">
            <h3>Model status</h3>
            <span className="chip chipAccent">{info?.ready ? "Ready" : "Not ready"}</span>
          </div>

          <div className="stack" style={{ gap: 8 }}>
            <Row label="In-memory version" value={info?.modelVersion ?? "—"} mono />
            <Row label="Registry name" value={info?.registryName ?? "—"} mono />
            <Row label="Registry format" value={info?.registryFormat ?? "—"} mono />
            <Row label="Registry version" value={info?.registryVersion ?? "—"} mono />
            <Row label="Last trained at" value={info?.lastTrainedAt ?? "—"} mono />
          </div>
        </div>

        <div className="card">
          <div className="cardTitle">
            <h3>Retrain</h3>
            <span className="muted">Admin action</span>
          </div>

          <div className="muted" style={{ marginBottom: 10 }}>
            Retrain using the last N feature rows. For the PoC this is an in-place refresh of the active model.
          </div>

          <div style={{ display: "flex", gap: 10, alignItems: "center", flexWrap: "wrap" }}>
            <label className="muted">
              Limit{" "}
              <input
                type="number"
                min={10}
                max={5000}
                value={limit}
                onChange={(e) => setLimit(parseInt(e.target.value || "500", 10))}
                style={{
                  width: 120,
                  marginLeft: 8,
                  padding: "8px 10px",
                  borderRadius: 10,
                  border: "1px solid rgba(255,255,255,0.12)",
                  background: "rgba(0,0,0,0.20)",
                  color: "var(--text)"
                }}
              />
            </label>
            <button type="button" className="btn btnPrimary" disabled={loading} onClick={handleRetrain}>
              {loading ? "Retraining…" : "Retrain model"}
            </button>
            <button type="button" className="btn" disabled={loading} onClick={loadInfo}>
              Refresh
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

function Row(props: { label: string; value: string; mono?: boolean }) {
  return (
    <div style={{ display: "flex", justifyContent: "space-between", gap: 14 }}>
      <span className="muted">{props.label}</span>
      <span className={props.mono ? "mono" : ""}>{props.value}</span>
    </div>
  );
}
