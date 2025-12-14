import React, { useEffect, useState } from "react";
import { API_BASE } from "../api";

type ModelInfo = {
  registryId: number | null;
  registryName: string | null;
  registryFormat: string | null;
  registryVersion: string | null;
  lastTrainedAt: string | null;
  now: string;
};

type Scorecard = {
  id: number;
  createdAt: string;
  triggerType: string;
  modelId: number | null;
  modelVersion: string | null;
  baselineN: number;
  recoveryN: number;
  status: string;
  baselineMetricsJson: string;
  recoveryMetricsJson: string;
  deltaMetricsJson: string;
  notes: string | null;
};

async function getJson<T>(url: string): Promise<T> {
  const r = await fetch(url);
  if (!r.ok) throw new Error(`${r.status} ${r.statusText}`);
  return r.json();
}
async function post(url: string): Promise<any> {
  const r = await fetch(url, { method: "POST" });
  if (!r.ok) throw new Error(`${r.status} ${r.statusText}`);
  return r.json().catch(() => ({}));
}

export function AdminMlView() {
  const [info, setInfo] = useState<ModelInfo | null>(null);
  const [jobs, setJobs] = useState<any[]>([]);
  const [scorecards, setScorecards] = useState<any[]>([]);
  const [selected, setSelected] = useState<Scorecard | null>(null);
  const [registry, setRegistry] = useState<any[]>([]);
  const [canary, setCanary] = useState<any | null>(null);
  const [changes, setChanges] = useState<any[]>([]);
  const [canaryModelId, setCanaryModelId] = useState<string>("0");
  const [canaryPercent, setCanaryPercent] = useState<string>("5");
  const [err, setErr] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function refresh() {
    setErr(null);
    setInfo(await getJson<ModelInfo>(`${API_BASE}/admin/model/info`));
    setJobs(await getJson<any[]>(`${API_BASE}/admin/model/jobs?limit=50`));
    setScorecards(await getJson<any[]>(`${API_BASE}/admin/model/scorecards?scopeType=GLOBAL&scopeKey=*&limit=50`));
    setRegistry(await getJson<any[]>(`${API_BASE}/admin/model/registry?kind=risk-model&scopeType=GLOBAL&scopeKey=*&limit=50`));
    setCanary(await getJson<any>(`${API_BASE}/admin/model/canary?scopeType=GLOBAL&scopeKey=*`));
    setChanges(await getJson<any[]>(`${API_BASE}/admin/model/changes?scopeType=GLOBAL&scopeKey=*&limit=100`));
  }

  useEffect(() => {
    refresh().catch((e) => setErr(e?.message || String(e)));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function enqueueRetrain() {
    setBusy(true);
    try {
      await post(`${API_BASE}/admin/model/retrain?reason=manual-ui`);
      await refresh();
    } catch (e: any) {
      setErr(e?.message || String(e));
    } finally {
      setBusy(false);
    }
  }

  async function retrainNow() {
    setBusy(true);
    try {
      await post(`${API_BASE}/admin/model/retrainNow?limit=500`);
      await refresh();
    } catch (e: any) {
      setErr(e?.message || String(e));
    } finally {
      setBusy(false);
    }
  }

  async function activateModel(id: number) {
    setBusy(true);
    try {
      await post(`${API_BASE}/admin/model/activate?modelId=${id}&actor=ui&reason=ui-activate`);
      await refresh();
    } catch (e: any) {
      setErr(e?.message || String(e));
    } finally {
      setBusy(false);
    }
  }

  async function rollback() {
    setBusy(true);
    try {
      await post(`${API_BASE}/admin/model/rollback?actor=ui&reason=ui-rollback&scopeType=GLOBAL&scopeKey=*`);
      await refresh();
    } catch (e: any) {
      setErr(e?.message || String(e));
    } finally {
      setBusy(false);
    }
  }

  async function openScorecard(id: number) {
    setBusy(true);
    try {
      const sc = await getJson<Scorecard>(`${API_BASE}/admin/model/scorecards/${id}`);
      setSelected(sc);
    } catch (e: any) {
      setErr(e?.message || String(e));
    } finally {
      setBusy(false);
    }
  }

  async function startCanary() {
    setBusy(true);
    try {
      await post(`${API_BASE}/admin/model/canary/start?modelId=${Number(canaryModelId)}&percent=${Number(canaryPercent)}&actor=ui&reason=ui-canary-start&scopeType=GLOBAL&scopeKey=*`);
      await refresh();
    } catch (e: any) {
      setErr(e?.message || String(e));
    } finally {
      setBusy(false);
    }
  }

  async function stepCanary(p: number) {
    setBusy(true);
    try {
      await post(`${API_BASE}/admin/model/canary/step?percent=${p}&actor=ui&reason=ui-canary-step&scopeType=GLOBAL&scopeKey=*`);
      await refresh();
    } catch (e: any) {
      setErr(e?.message || String(e));
    } finally {
      setBusy(false);
    }
  }

  async function stopCanary() {
    setBusy(true);
    try {
      await post(`${API_BASE}/admin/model/canary/stop?actor=ui&reason=ui-canary-stop&scopeType=GLOBAL&scopeKey=*`);
      await refresh();
    } catch (e: any) {
      setErr(e?.message || String(e));
    } finally {
      setBusy(false);
    }
  }

  const canaryText = canary
    ? `enabled=${String(canary?.enabled)} modelId=${String(canary?.modelId)} rollout=${String(canary?.rolloutPercent)}%`
    : "none";

  const sectionStyle: React.CSSProperties = {
    border: "1px solid rgba(255,255,255,0.12)",
    borderRadius: 12,
    padding: 14,
    background: "rgba(255,255,255,0.04)",
  };

  const tableStyle: React.CSSProperties = { width: "100%", borderCollapse: "collapse" };
  const thtd: React.CSSProperties = { textAlign: "left", padding: "8px 6px", borderBottom: "1px solid rgba(255,255,255,0.12)" };

  return (
    <div style={{ display: "grid", gap: 14 }}>
      <div style={sectionStyle}>
        <div style={{ display: "flex", justifyContent: "space-between", gap: 12, flexWrap: "wrap" }}>
          <div>
            <h2 style={{ margin: 0 }}>Admin / ML Model</h2>
            <div style={{ opacity: 0.8, marginTop: 4 }}>
              EPIC 11.7–11.9 · Retraining + KPI scorecards + canary rollouts + auto rollback
            </div>
          </div>
          <div style={{ display: "flex", gap: 8, flexWrap: "wrap", alignItems: "center" }}>
            <button onClick={refresh} disabled={busy}>Refresh</button>
            <button onClick={enqueueRetrain} disabled={busy}>Enqueue retrain</button>
            <button onClick={retrainNow} disabled={busy}>Retrain now</button>
            <button onClick={rollback} disabled={busy}>Rollback</button>
          </div>
        </div>

        {err && (
          <div style={{ marginTop: 10, padding: 10, borderRadius: 10, border: "1px solid rgba(255,80,80,0.6)" }}>
            Error: {err}
          </div>
        )}

        <div style={{ display: "flex", gap: 18, flexWrap: "wrap", marginTop: 10 }}>
          <div><b>Active:</b> {info?.registryVersion ?? "none"}</div>
          <div><b>Format:</b> {info?.registryFormat ?? "—"}</div>
          <div><b>Last trained:</b> {info?.lastTrainedAt ?? "—"}</div>
          <div><b>Canary:</b> {canaryText}</div>
        </div>

        <div style={{ marginTop: 10, display: "flex", gap: 8, flexWrap: "wrap", alignItems: "center" }}>
          <b>Canary controls:</b>
          <label style={{ display: "flex", gap: 6, alignItems: "center" }}>
            ModelId
            <input value={canaryModelId} onChange={(e) => setCanaryModelId(e.target.value)} style={{ width: 110 }} />
          </label>
          <label style={{ display: "flex", gap: 6, alignItems: "center" }}>
            %
            <input value={canaryPercent} onChange={(e) => setCanaryPercent(e.target.value)} style={{ width: 70 }} />
          </label>
          <button onClick={startCanary} disabled={busy}>Start/Update</button>
          <button onClick={() => stepCanary(5)} disabled={busy}>5%</button>
          <button onClick={() => stepCanary(25)} disabled={busy}>25%</button>
          <button onClick={() => stepCanary(50)} disabled={busy}>50%</button>
          <button onClick={() => stepCanary(100)} disabled={busy}>100%</button>
          <button onClick={stopCanary} disabled={busy}>Stop</button>
        </div>
      </div>

      <div style={sectionStyle}>
        <h3 style={{ marginTop: 0 }}>Model registry</h3>
        <table style={tableStyle}>
          <thead>
            <tr>
              <th style={thtd}>id</th>
              <th style={thtd}>created</th>
              <th style={thtd}>version</th>
              <th style={thtd}>format</th>
              <th style={thtd}>active</th>
              <th style={thtd}></th>
            </tr>
          </thead>
          <tbody>
            {registry.map((m: any) => (
              <tr key={m.id}>
                <td style={thtd}>{m.id}</td>
                <td style={thtd}>{m.createdAt}</td>
                <td style={thtd}>{m.version}</td>
                <td style={thtd}>{m.format}</td>
                <td style={thtd}>{String(m.active)}</td>
                <td style={thtd}>
                  <button onClick={() => activateModel(m.id)} disabled={busy || m.active}>Activate</button>
                </td>
              </tr>
            ))}
            {registry.length === 0 && (
              <tr><td style={thtd} colSpan={6}>No models yet (run retrain).</td></tr>
            )}
          </tbody>
        </table>
      </div>

      <div style={sectionStyle}>
        <h3 style={{ marginTop: 0 }}>Scorecards</h3>
        <table style={tableStyle}>
          <thead>
            <tr>
              <th style={thtd}>id</th>
              <th style={thtd}>created</th>
              <th style={thtd}>trigger</th>
              <th style={thtd}>modelId</th>
              <th style={thtd}>status</th>
              <th style={thtd}></th>
            </tr>
          </thead>
          <tbody>
            {scorecards.map((s: any) => (
              <tr key={s.id}>
                <td style={thtd}>{s.id}</td>
                <td style={thtd}>{s.createdAt}</td>
                <td style={thtd}>{s.triggerType}</td>
                <td style={thtd}>{s.modelId ?? ""}</td>
                <td style={thtd}>{s.status}</td>
                <td style={thtd}><button onClick={() => openScorecard(s.id)} disabled={busy}>Open</button></td>
              </tr>
            ))}
            {scorecards.length === 0 && (
              <tr><td style={thtd} colSpan={6}>No scorecards yet.</td></tr>
            )}
          </tbody>
        </table>

        {selected && (
          <div style={{ marginTop: 12 }}>
            <h4 style={{ margin: "8px 0" }}>Scorecard #{selected.id} ({selected.status})</h4>
            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr 1fr", gap: 12 }}>
              <div>
                <b>Baseline</b>
                <pre style={{ fontSize: 12, whiteSpace: "pre-wrap" }}>{JSON.stringify(JSON.parse(selected.baselineMetricsJson), null, 2)}</pre>
              </div>
              <div>
                <b>Recovery</b>
                <pre style={{ fontSize: 12, whiteSpace: "pre-wrap" }}>{JSON.stringify(JSON.parse(selected.recoveryMetricsJson), null, 2)}</pre>
              </div>
              <div>
                <b>Delta</b>
                <pre style={{ fontSize: 12, whiteSpace: "pre-wrap" }}>{JSON.stringify(JSON.parse(selected.deltaMetricsJson), null, 2)}</pre>
              </div>
            </div>
          </div>
        )}
      </div>

      <div style={sectionStyle}>
        <h3 style={{ marginTop: 0 }}>Change history</h3>
        <table style={tableStyle}>
          <thead>
            <tr>
              <th style={thtd}>created</th>
              <th style={thtd}>event</th>
              <th style={thtd}>from</th>
              <th style={thtd}>to</th>
              <th style={thtd}>reason</th>
            </tr>
          </thead>
          <tbody>
            {changes.map((c: any, idx: number) => (
              <tr key={idx}>
                <td style={thtd}>{c.createdAt}</td>
                <td style={thtd}>{c.eventType}</td>
                <td style={thtd}>{c.fromModelId ?? ""}</td>
                <td style={thtd}>{c.toModelId ?? ""}</td>
                <td style={thtd}>{c.reason ?? ""}</td>
              </tr>
            ))}
            {changes.length === 0 && (
              <tr><td style={thtd} colSpan={5}>No change events yet.</td></tr>
            )}
          </tbody>
        </table>
      </div>

      <div style={sectionStyle}>
        <h3 style={{ marginTop: 0 }}>Retrain jobs</h3>
        <table style={tableStyle}>
          <thead>
            <tr>
              <th style={thtd}>id</th>
              <th style={thtd}>created</th>
              <th style={thtd}>status</th>
              <th style={thtd}>from</th>
              <th style={thtd}>to</th>
              <th style={thtd}>reason</th>
              <th style={thtd}>error</th>
            </tr>
          </thead>
          <tbody>
            {jobs.map((j: any) => (
              <tr key={j.id}>
                <td style={thtd}>{j.id}</td>
                <td style={thtd}>{j.createdAt}</td>
                <td style={thtd}>{j.status}</td>
                <td style={thtd}>{j.fromModelId ?? ""}</td>
                <td style={thtd}>{j.toModelId ?? ""}</td>
                <td style={thtd}>{j.reason ?? ""}</td>
                <td style={thtd}>{j.error ?? ""}</td>
              </tr>
            ))}
            {jobs.length === 0 && (
              <tr><td style={thtd} colSpan={7}>No jobs yet.</td></tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
