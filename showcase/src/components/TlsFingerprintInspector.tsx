import React, { useEffect, useState } from "react";
import type { TlsFingerprintStats } from "../api";
import { fetchTlsFingerprintStats } from "../api";

interface Props {
  tlsFp?: string | null;
}

export function TlsFingerprintInspector({ tlsFp }: Props) {
  const [stats, setStats] = useState<TlsFingerprintStats | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    const run = async () => {
      if (!tlsFp || tlsFp === "none") {
        setStats(null);
        return;
      }
      setLoading(true);
      setError(null);
      try {
        const data = await fetchTlsFingerprintStats(tlsFp);
        if (!cancelled) {
          setStats(data);
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
  }, [tlsFp]);

  return (
    <div style={cardStyle}>
      <h2>TLS fingerprint inspector</h2>
      {!tlsFp || tlsFp === "none" ? (
        <p style={{ fontSize: "0.85rem" }}>
          No TLS fingerprint header was present. Make sure you are calling the API via the TLS gateway.
        </p>
      ) : (
        <>
          <p style={{ fontSize: "0.85rem", marginBottom: "0.5rem" }}>
            Current TLS fingerprint:
            <code style={{ marginLeft: 6, fontSize: "0.8rem" }}>{tlsFp}</code>
          </p>
          {loading && <p>Loading TLS history…</p>}
          {error && <p style={{ color: "red" }}>{error}</p>}
          {!loading && !error && stats && (
            <ul style={{ paddingLeft: "1.1rem", fontSize: "0.85rem" }}>
              <li>Profiles with this TLS FP: {stats.profiles}</li>
              <li>Distinct users seen: {stats.users}</li>
              <li>First seen: {new Date(stats.firstSeen).toLocaleString()}</li>
              <li>Last seen: {new Date(stats.lastSeen).toLocaleString()}</li>
            </ul>
          )}
          {!loading && !error && !stats && (
            <p style={{ fontSize: "0.85rem" }}>
              This TLS fingerprint is not yet in the device_profile table. Run a few checks to populate it.
            </p>
          )}
        </>
      )}
    </div>
  );
}

const cardStyle: React.CSSProperties = {
  border: "1px solid #ddd",
  borderRadius: 8,
  padding: "1rem",
  background: "#fff",
  boxShadow: "0 1px 3px rgba(0,0,0,0.05)",
  minWidth: 260,
  flex: 1
};
