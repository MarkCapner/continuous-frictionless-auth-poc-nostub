import React, { useEffect, useState } from "react";
import type { TlsFpOverview } from "../api";
import { fetchTlsFpOverview } from "../api";

export interface TlsFpVisualizerProps {
  userHint: string;
  preselectedFp?: string;
}

/**
 * Mini-Epic 8F: TLS fingerprint visualiser.
 *
 * Uses /showcase/tls-fp/overview to display rarity and associated devices for a given TLS FP.
 * Integrates with the device history panel via the preselectedFp prop.
 */
export function TlsFpVisualizer({ userHint, preselectedFp }: TlsFpVisualizerProps) {
  const [inputFp, setInputFp] = useState<string>("");
  const [overview, setOverview] = useState<TlsFpOverview | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // When the parent selects a TLS FP (e.g. from DeviceHistoryCharts), sync & auto-fetch.
  useEffect(() => {
    if (!preselectedFp) return;
    setInputFp(preselectedFp);
    void runFetch(preselectedFp);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [preselectedFp]);

  async function runFetch(fpRaw?: string) {
    const fp = (fpRaw ?? inputFp).trim();
    if (!fp) return;
    setLoading(true);
    setError(null);
    setOverview(null);
    try {
      const data = await fetchTlsFpOverview(fp);
      setOverview(data);
    } catch (e: any) {
      setError(e?.message ?? String(e));
    } finally {
      setLoading(false);
    }
  }

  const handleSubmit: React.FormEventHandler = (e) => {
    e.preventDefault();
    void runFetch();
  };

  return (
    <div style={cardStyle}>
      <h3 style={titleStyle}>TLS fingerprint visualiser</h3>
      <p style={bodyStyle}>
        Explore TLS fingerprints observed for <code>{(userHint || "demo-user").trim() || "demo-user"}</code>. You can
        either type a fingerprint hash or click a TLS FP pill from the device history panel above.
      </p>

      <form onSubmit={handleSubmit} style={formRowStyle}>
        <input
          type="text"
          value={inputFp}
          onChange={(e) => setInputFp(e.target.value)}
          placeholder="Enter TLS fingerprint hash"
          style={inputStyle}
        />
        <button type="submit" disabled={loading || !inputFp.trim()} style={buttonStyle}>
          {loading ? "Loading…" : "Visualise"}
        </button>
      </form>

      {error && (
        <p style={{ ...bodyStyle, color: "#b91c1c" }}>
          Error loading TLS FP: {error}
        </p>
      )}

      {!loading && !error && !overview && !inputFp.trim() && (
        <p style={hintStyle}>Select a TLS FP from device history or enter one manually to see details.</p>
      )}

      {!loading && !error && inputFp.trim() && !overview && (
        <p style={hintStyle}>No data loaded yet. Click Visualise to query the backend.</p>
      )}

      {overview && (
        <div style={{ marginTop: "0.75rem", display: "flex", flexDirection: "column", gap: "0.5rem" }}>
          <section>
            <h4 style={subTitleStyle}>Overview</h4>
            <p style={bodyStyle}>
              <strong>TLS FP:</strong> <code>{overview.tlsFp}</code>
              <br />
              <strong>Profiles:</strong> {overview.profiles} · <strong>Users:</strong> {overview.users}
            </p>
            <p style={hintStyle}>
              First seen {formatDate(overview.firstSeen)} · last seen {formatDate(overview.lastSeen)}
            </p>
          </section>

          <section>
            <h4 style={subTitleStyle}>Associated devices</h4>
            {overview.devices.length === 0 ? (
              <p style={bodyStyle}>No device profiles attached to this fingerprint yet.</p>
            ) : (
              <table style={tableStyle}>
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>User</th>
                    <th>User agent</th>
                    <th>Country</th>
                    <th>First seen</th>
                    <th>Last seen</th>
                  </tr>
                </thead>
                <tbody>
                  {overview.devices.map((d) => (
                    <tr key={d.id}>
                      <td>#{d.id}</td>
                      <td>{d.userId}</td>
                      <td>
                        {d.uaFamily} {d.uaVersion}
                      </td>
                      <td>{d.lastCountry ?? "–"}</td>
                      <td>{formatDate(d.firstSeen)}</td>
                      <td>{formatDate(d.lastSeen)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </section>
        </div>
      )}
    </div>
  );
}

function formatDate(value: string | null): string {
  if (!value) return "unknown";
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return value;
  return d.toISOString().slice(0, 10);
}

const cardStyle: React.CSSProperties = {
  borderRadius: 8,
  borderWidth: 1,
  borderStyle: "solid",
  borderColor: "#e5e7eb",
  background: "#fff",
  padding: "0.75rem 1rem",
  minHeight: 180,
  boxSizing: "border-box"
};

const titleStyle: React.CSSProperties = {
  margin: "0 0 0.35rem"
};

const bodyStyle: React.CSSProperties = {
  margin: "0.25rem 0"
};

const hintStyle: React.CSSProperties = {
  margin: "0.25rem 0",
  fontSize: "0.8rem",
  color: "#6b7280"
};

const subTitleStyle: React.CSSProperties = {
  margin: "0.35rem 0 0.25rem",
  fontSize: "0.9rem"
};

const formRowStyle: React.CSSProperties = {
  display: "flex",
  gap: "0.5rem",
  alignItems: "center",
  marginTop: "0.35rem"
};

const inputStyle: React.CSSProperties = {
  flex: 1,
  padding: "0.3rem 0.5rem",
  borderRadius: 4,
  borderWidth: 1,
  borderStyle: "solid",
  borderColor: "#d1d5db",
  fontSize: "0.85rem"
};

const buttonStyle: React.CSSProperties = {
  fontSize: "0.8rem",
  padding: "0.3rem 0.7rem",
  borderRadius: 999,
  borderWidth: 1,
  borderStyle: "solid",
  borderColor: "#4f46e5",
  background: "#4f46e5",
  color: "#fff",
  cursor: "pointer",
  whiteSpace: "nowrap"
};

const tableStyle: React.CSSProperties = {
  width: "100%",
  borderCollapse: "collapse",
  fontSize: "0.8rem",
  marginTop: "0.25rem"
};
