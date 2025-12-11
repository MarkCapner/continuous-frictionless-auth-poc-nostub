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

  useEffect(() => {
    void loadInfo();
  }, []);

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
    <section style={containerStyle}>
      <h2>Admin / ML Model</h2>
      <p style={{ maxWidth: 640 }}>
        This panel shows the currently active ML model (as recorded in the{" "}
        <code>model_registry</code> table) and lets you trigger a retrain from recent{" "}
        <code>session_feature</code> rows directly from the browser.
      </p>

      <div style={cardStyle}>
        <div style={rowStyle}>
          <span>Model ready:</span>
          <strong>{info?.ready ? "Yes" : "No"}</strong>
        </div>
        <div style={rowStyle}>
          <span>In-memory version:</span>
          <code>{info?.modelVersion ?? "unknown"}</code>
        </div>
        <div style={rowStyle}>
          <span>Registry name:</span>
          <code>{info?.registryName ?? "—"}</code>
        </div>
        <div style={rowStyle}>
          <span>Registry format:</span>
          <code>{info?.registryFormat ?? "—"}</code>
        </div>
        <div style={rowStyle}>
          <span>Registry version:</span>
          <code>{info?.registryVersion ?? "—"}</code>
        </div>
        <div style={rowStyle}>
          <span>Last trained at:</span>
          <code>{info?.lastTrainedAt ?? "—"}</code>
        </div>
      </div>

      <div style={{ marginTop: "1rem", display: "flex", gap: "0.75rem", alignItems: "center" }}>
        <label>
          Retrain using last{" "}
          <input
            type="number"
            min={10}
            max={5000}
            value={limit}
            onChange={(e) => setLimit(Number(e.target.value) || 0)}
            style={{ width: 90, margin: "0 0.25rem" }}
          />
          sessions
        </label>
        <button
          type="button"
          onClick={handleRetrain}
          disabled={loading || limit <= 0}
          style={buttonStyle}
        >
          {loading ? "Retraining..." : "Retrain model"}
        </button>
        <button type="button" onClick={loadInfo} disabled={loading} style={secondaryButtonStyle}>
          Refresh
        </button>
      </div>

      {error && (
        <p style={{ color: "#b91c1c", marginTop: "0.75rem" }}>
          Error: {error}
        </p>
      )}
    </section>
  );
}

const containerStyle: React.CSSProperties = {
  display: "flex",
  flexDirection: "column",
  gap: "0.75rem"
};

const cardStyle: React.CSSProperties = {
  borderRadius: 8,
  border: "1px solid #e5e7eb",
  padding: "0.75rem 1rem",
  background: "#f9fafb",
  maxWidth: 480
};

const rowStyle: React.CSSProperties = {
  display: "flex",
  justifyContent: "space-between",
  gap: "0.75rem",
  padding: "0.15rem 0",
  fontSize: "0.9rem"
};

const buttonStyle: React.CSSProperties = {
  padding: "0.4rem 0.9rem",
  borderRadius: 999,
  border: "none",
  cursor: "pointer",
  background: "#4f46e5",
  color: "#fff",
  fontSize: "0.9rem"
};

const secondaryButtonStyle: React.CSSProperties = {
  ...buttonStyle,
  background: "#e5e7eb",
  color: "#111827"
};
