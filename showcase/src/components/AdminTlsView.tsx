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
        const data = await fetchAllTlsFingerprintSummaries(100);
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

    run();
    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <div style={pageStyle}>
      <header style={headerStyle}>
        <div>
          <h1 style={{ margin: 0 }}>Admin / TLS fingerprints</h1>
          <p style={{ margin: 0, fontSize: "0.9rem", color: "#4b5563" }}>
            Explore all known TLS fingerprints in the device_profile table and drill into each one.
          </p>
        </div>
      </header>

      <div style={gridStyle}>
        <div style={tableCardStyle}>
          <h2 style={{ marginTop: 0 }}>Fingerprints</h2>
          {loading && <p>Loading TLS fingerprints…</p>}
          {error && <p style={{ color: "red" }}>{error}</p>}
          {!loading && !error && summaries.length === 0 && (
            <p style={{ fontSize: "0.9rem" }}>No fingerprints yet. Run some profile checks first.</p>
          )}
          {!loading && !error && summaries.length > 0 && (
            <div style={{ maxHeight: 360, overflowY: "auto", marginTop: "0.5rem" }}>
              <table style={tableStyle}>
                <thead>
                  <tr>
                    <th style={thStyle}>TLS FP</th>
                    <th style={thStyle}>Profiles</th>
                    <th style={thStyle}>Users</th>
                    <th style={thStyle}>First seen</th>
                    <th style={thStyle}>Last seen</th>
                  </tr>
                </thead>
                <tbody>
                  {summaries.map((s) => (
                    <tr
                      key={s.tlsFp}
                      onClick={() => setSelectedFp(s.tlsFp)}
                      style={
                        s.tlsFp === selectedFp
                          ? { ...trStyle, background: "#eff6ff" }
                          : trStyle
                      }
                    >
                      <td style={tdStyle}>
                        <code>{s.tlsFp}</code>
                      </td>
                      <td style={tdStyle}>{s.profiles}</td>
                      <td style={tdStyle}>{s.users}</td>
                      <td style={tdStyle}>{new Date(s.firstSeen).toLocaleString()}</td>
                      <td style={tdStyle}>{new Date(s.lastSeen).toLocaleString()}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
        <div style={detailCardStyle}>
          <TlsFingerprintInspector tlsFp={selectedFp} />
        </div>
      </div>
    </div>
  );
}

const pageStyle: React.CSSProperties = {
  maxWidth: 1200,
  margin: "0 auto",
  padding: "1.5rem 1.25rem 2rem"
};

const headerStyle: React.CSSProperties = {
  marginBottom: "1rem",
  display: "flex",
  alignItems: "baseline",
  justifyContent: "space-between",
  gap: "1rem"
};

const gridStyle: React.CSSProperties = {
  display: "grid",
  gridTemplateColumns: "1.3fr 1fr",
  gap: "1.25rem",
  alignItems: "flex-start"
};

const tableCardStyle: React.CSSProperties = {
  border: "1px solid #ddd",
  borderRadius: 8,
  padding: "1rem",
  background: "#fff",
  boxShadow: "0 1px 3px rgba(0,0,0,0.05)"
};

const detailCardStyle: React.CSSProperties = {
  minWidth: 0
};

const tableStyle: React.CSSProperties = {
  width: "100%",
  borderCollapse: "collapse",
  fontSize: "0.8rem"
};

const thStyle: React.CSSProperties = {
  textAlign: "left",
  padding: "0.25rem 0.35rem",
  borderBottom: "1px solid #e5e7eb",
  position: "sticky",
  top: 0,
  background: "#f9fafb",
  zIndex: 1
};

const trStyle: React.CSSProperties = {
  cursor: "pointer"
};

const tdStyle: React.CSSProperties = {
  padding: "0.25rem 0.35rem",
  borderBottom: "1px solid #f3f4f6",
  verticalAlign: "top"
};
