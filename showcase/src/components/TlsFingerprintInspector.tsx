import React, { useEffect, useState } from "react";
import type { TlsFamilyDetails, TlsFingerprintStats } from "../api";
import { fetchTlsFamilyDetailsByFp, fetchTlsFingerprintStats } from "../api";

interface Props {
  tlsFp?: string | null;
}

export function TlsFingerprintInspector({ tlsFp }: Props) {
  const [stats, setStats] = useState<TlsFingerprintStats | null>(null);
  const [family, setFamily] = useState<TlsFamilyDetails | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    const run = async () => {
      if (!tlsFp || tlsFp === "none") {
        setStats(null);
        setFamily(null);
        return;
      }
      setLoading(true);
      setError(null);
      try {
        const data = await fetchTlsFingerprintStats(tlsFp);
        if (!cancelled) {
          setStats(data);
        }
        const fam = await fetchTlsFamilyDetailsByFp(tlsFp, 12);
        if (!cancelled) {
          setFamily(fam);
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
    <div className="card">
      <div className="cardTitle">
        <h3>TLS fingerprint</h3>
        {tlsFp && tlsFp !== "none" ? <span className="chip chipAccent">live</span> : <span className="chip">no header</span>}
      </div>
      {!tlsFp || tlsFp === "none" ? (
        <p className="muted" style={{ fontSize: "0.85rem" }}>
          No TLS fingerprint header was present. Make sure you are calling the API via the TLS gateway.
        </p>
      ) : (
        <>
          <div className="mono" style={{ wordBreak: "break-all" }}>{tlsFp}</div>
          {loading && <p className="muted" style={{ marginTop: 10 }}>Loading TLS history…</p>}
          {error && <p style={{ color: "var(--danger)", marginTop: 10 }}>{error}</p>}
          {!loading && !error && stats && (
            <div style={{ display: "flex", flexWrap: "wrap", gap: 8, marginTop: 10 }}>
              <span className="chip">profiles · {stats.profiles}</span>
              <span className="chip">users · {stats.users}</span>
              <span className="chip">first · {new Date(stats.firstSeen).toLocaleDateString()}</span>
              <span className="chip">last · {new Date(stats.lastSeen).toLocaleDateString()}</span>
            </div>
          )}
          {!loading && !error && family && (
            <div style={{ marginTop: 12 }}>
              <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: 10 }}>
                <div className="muted" style={{ fontSize: 12 }}>Family</div>
                <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
                  <span className="chip chipAccent2">variants · {family.variants.length}</span>
                  <span className="chip">users · {family.users}</span>
                </div>
              </div>
              <div style={{ marginTop: 8 }}>
                <div className="mono">{family.familyId}</div>
                <div className="muted" style={{ fontSize: 12, marginTop: 8 }}>
                  Subject CN: <span className="mono">{family.subject?.CN ?? ""}</span>
                </div>
                <div className="muted" style={{ fontSize: 12, marginTop: 6 }}>
                  Issuer CN: <span className="mono">{family.issuer?.CN ?? ""}</span>
                </div>
              </div>

              <details style={{ marginTop: 10 }}>
                <summary style={{ cursor: "pointer", color: "rgba(255,255,255,0.86)" }}>Show family key & variants</summary>
                <div style={{ marginTop: 10 }}>
                  <div className="muted" style={{ fontSize: 12 }}>Family key</div>
                  <div className="mono" style={{ whiteSpace: "pre-wrap" }}>{family.familyKey}</div>
                  <div className="muted" style={{ fontSize: 12, marginTop: 10 }}>Recent variants</div>
                  <div style={{ display: "flex", flexWrap: "wrap", gap: 6 }}>
                    {family.variants.map((v) => (
                      <span key={v} className="chip">{v}</span>
                    ))}
                  </div>
                </div>
              </details>
            </div>
          )}
          {!loading && !error && !stats && (
            <div style={{ marginTop: 10 }}>
              <div className="chip">Family not observed yet</div>
              <p className="muted" style={{ fontSize: "0.85rem", marginTop: 8 }}>
                Run a few profile checks to populate TLS history, then revisit this panel.
              </p>
            </div>
          )}
        </>
      )}
    </div>
  );
}
