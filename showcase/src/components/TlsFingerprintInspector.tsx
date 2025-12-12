import React, { useEffect, useState } from "react";
import type { TlsFamilyShowcaseResponse, TlsFingerprintStats, TlsFamilyBackfillResponse } from "../api";
import { backfillTlsFamilies, fetchTlsFamilyDetailsByFp, fetchTlsFingerprintStats, forceClassifyTlsFamily } from "../api";

interface Props {
  tlsFp?: string | null;
}

export function TlsFingerprintInspector({ tlsFp }: Props) {
  const [stats, setStats] = useState<TlsFingerprintStats | null>(null);
  const [family, setFamily] = useState<TlsFamilyShowcaseResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // EPIC 9.1.3 (admin-only): in-memory admin token + optional tls meta for force classify.
  const [adminToken, setAdminToken] = useState<string>("");
  const [tlsMeta, setTlsMeta] = useState<string>("");
  const [forcing, setForcing] = useState(false);
  const [forceMsg, setForceMsg] = useState<string | null>(null);

  // EPIC 9.1.4/9.1.5: admin-triggered backfill.
  const [backfilling, setBackfilling] = useState(false);
  const [backfillRes, setBackfillRes] = useState<TlsFamilyBackfillResponse | null>(null);
  const [backfillMsg, setBackfillMsg] = useState<string | null>(null);

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

  const doForceClassify = async () => {
    if (!tlsFp || tlsFp === "none") return;
    setForceMsg(null);
    if (!adminToken.trim()) {
      setForceMsg("Admin token required.");
      return;
    }
    if (!tlsMeta.trim()) {
      setForceMsg("TLS meta required (subject/issuer). Paste the X-TLS-Meta value.");
      return;
    }
    setForcing(true);
    try {
      const updated = await forceClassifyTlsFamily(tlsFp, tlsMeta, adminToken.trim(), 12);
      setFamily(updated);
      setForceMsg("Classified. TLS family panel updated.");
    } catch (e: any) {
      setForceMsg(e?.message ?? String(e));
    } finally {
      setForcing(false);
    }
  };

  const doBackfill = async () => {
    setBackfillMsg(null);
    setBackfillRes(null);
    if (!adminToken.trim()) {
      setBackfillMsg("Admin token required.");
      return;
    }
    setBackfilling(true);
    try {
      const res = await backfillTlsFamilies(adminToken.trim(), 500, 20);
      setBackfillRes(res);
      setBackfillMsg(
        `Backfill complete: processed=${res.processed}, classified=${res.classified}, batches=${res.batches}${res.complete ? "" : " (paused)"}`
      );
    } catch (e: any) {
      setBackfillMsg(e?.message ?? String(e));
    } finally {
      setBackfilling(false);
    }
  };

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
          {!loading && !error && family && !family.notObserved && (
            <div style={{ marginTop: "0.5rem", fontSize: "0.85rem" }}>
              <h3 style={{ margin: "0.5rem 0 0.25rem", fontSize: "0.95rem" }}>TLS family</h3>
              <ul style={{ paddingLeft: "1.1rem", margin: 0 }}>
                <li>
                  Family ID: <code style={{ fontSize: "0.78rem" }}>{family.familyId}</code>
                </li>
                <li>
                  Variants: {family.variants.length} · Users (by family): {family.users ?? 0}
                </li>
                <li>
                  Subject CN: <code style={{ fontSize: "0.78rem" }}>{family.subject?.CN ?? ""}</code>
                </li>
                <li>
                  Issuer CN: <code style={{ fontSize: "0.78rem" }}>{family.issuer?.CN ?? ""}</code>
                </li>
              </ul>
              <details style={{ marginTop: "0.35rem" }}>
                <summary style={{ cursor: "pointer" }}>Show family key & variants</summary>
                <div style={{ marginTop: "0.35rem" }}>
                  <div style={{ fontSize: "0.8rem", color: "#4b5563" }}>Family key</div>
                  <code style={{ display: "block", fontSize: "0.75rem", whiteSpace: "pre-wrap" }}>{family.familyKey}</code>
                  <div style={{ marginTop: "0.35rem", fontSize: "0.8rem", color: "#4b5563" }}>Recent variants</div>
                  <div style={{ display: "flex", flexWrap: "wrap", gap: "0.25rem" }}>
                    {family.variants.map((v) => (
                      <code key={v} style={{ fontSize: "0.72rem", padding: "0.1rem 0.35rem", border: "1px solid #e5e7eb", borderRadius: 999 }}>
                        {v}
                      </code>
                    ))}
                  </div>
                </div>
              </details>

              <details style={{ marginTop: "0.75rem" }}>
                <summary style={{ cursor: "pointer", fontWeight: 600 }}>Admin tools</summary>
                <div style={{ marginTop: "0.5rem" }}>
                  <label style={{ display: "block", fontSize: "0.8rem", color: "#475569", marginBottom: 4 }}>
                    Admin token
                  </label>
                  <input
                    value={adminToken}
                    onChange={(e) => setAdminToken(e.target.value)}
                    placeholder="Enter X-Admin-Token"
                    style={{ width: "100%", padding: "0.4rem 0.5rem", borderRadius: 6, border: "1px solid #e2e8f0" }}
                  />

                  <div style={{ display: "flex", gap: "0.5rem", alignItems: "center", marginTop: "0.5rem" }}>
                    <button
                      disabled={backfilling || adminToken.trim().length === 0}
                      onClick={doBackfill}
                      style={{
                        padding: "0.45rem 0.6rem",
                        borderRadius: 8,
                        border: "1px solid #cbd5e1",
                        background: backfilling ? "#f1f5f9" : "#fff",
                        cursor: backfilling ? "not-allowed" : "pointer"
                      }}
                    >
                      {backfilling ? "Backfilling…" : "Backfill historical families"}
                    </button>
                    {backfillMsg && <span style={{ fontSize: "0.8rem", color: "#475569" }}>{backfillMsg}</span>}
                  </div>
                </div>
              </details>

            </div>
          )}
          {!loading && !error && family && family.notObserved && (
            <div style={{ marginTop: "0.5rem", fontSize: "0.85rem" }}>
              <h3 style={{ margin: "0.5rem 0 0.25rem", fontSize: "0.95rem" }}>TLS family</h3>
              <div
                style={{
                  border: "1px dashed #cbd5e1",
                  background: "#f8fafc",
                  borderRadius: 8,
                  padding: "0.75rem"
                }}
              >
                <div style={{ fontWeight: 600, marginBottom: "0.25rem" }}>Family not yet observed</div>
                <div style={{ color: "#475569", lineHeight: 1.35 }}>
                  This TLS fingerprint hasn’t been clustered into a family yet.
                </div>
                <ul style={{ margin: "0.5rem 0 0", paddingLeft: "1.1rem", color: "#475569" }}>
                  <li>Generate a few more sessions so the system can observe variants.</li>
                  <li>If you’re an admin, you can force normalise &amp; classify this FP into a family.</li>
                </ul>

                <div style={{ marginTop: "0.75rem" }}>
                  <details>
                    <summary style={{ cursor: "pointer", fontWeight: 600 }}>Admin tools</summary>
                    <div style={{ marginTop: "0.5rem" }}>
                      <label style={{ display: "block", fontSize: "0.8rem", color: "#475569", marginBottom: 4 }}>
                        Admin token
                      </label>
                      <input
                        value={adminToken}
                        onChange={(e) => setAdminToken(e.target.value)}
                        placeholder="Enter X-Admin-Token"
                        style={{ width: "100%", padding: "0.4rem 0.5rem", borderRadius: 6, border: "1px solid #e2e8f0" }}
                      />

                      <label style={{ display: "block", fontSize: "0.8rem", color: "#475569", margin: "0.5rem 0 4px" }}>
                        TLS meta (subject/issuer) — required
                      </label>
                      <textarea
                        value={tlsMeta}
                        onChange={(e) => setTlsMeta(e.target.value)}
                        placeholder="Paste the X-TLS-Meta value here (must include sub= and iss= fields)"
                        rows={4}
                        style={{ width: "100%", padding: "0.4rem 0.5rem", borderRadius: 6, border: "1px solid #e2e8f0", fontFamily: "monospace", fontSize: "0.78rem" }}
                      />

                      <div style={{ display: "flex", gap: "0.5rem", alignItems: "center", marginTop: "0.5rem" }}>
                        <button
                          disabled={forcing || adminToken.trim().length === 0 || tlsMeta.trim().length === 0}
                          onClick={doForceClassify}
                          style={{
                            padding: "0.45rem 0.6rem",
                            borderRadius: 8,
                            border: "1px solid #cbd5e1",
                            background: forcing ? "#f1f5f9" : "#fff",
                            cursor: forcing ? "not-allowed" : "pointer"
                          }}
                        >
                          {forcing ? "Classifying…" : "Force normalise & classify"}
                        </button>
                        {forceMsg && <span style={{ fontSize: "0.8rem", color: "#475569" }}>{forceMsg}</span>}
                      </div>

                      <div style={{ display: "flex", gap: "0.5rem", alignItems: "center", marginTop: "0.5rem" }}>
                        <button
                          disabled={backfilling || adminToken.trim().length === 0}
                          onClick={doBackfill}
                          style={{
                            padding: "0.45rem 0.6rem",
                            borderRadius: 8,
                            border: "1px solid #cbd5e1",
                            background: backfilling ? "#f1f5f9" : "#fff",
                            cursor: backfilling ? "not-allowed" : "pointer"
                          }}
                        >
                          {backfilling ? "Backfilling…" : "Backfill historical families"}
                        </button>
                        {backfillMsg && <span style={{ fontSize: "0.8rem", color: "#475569" }}>{backfillMsg}</span>}
                      </div>
                    </div>
                  </details>
                </div>
              </div>
            </div>
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
