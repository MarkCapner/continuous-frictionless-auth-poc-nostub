import React, { useEffect, useState } from "react";
import { API_BASE } from "../api";
import { SummaryCards } from "../ui/SummaryCards";
import { ExpandablePanel } from "../ui/ExpandablePanel";
import { JsonOptIn } from "../ui/JsonOptIn";

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
  const [policyMatches, setPolicyMatches] = useState<any[]>([]);
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
    setPolicyMatches(await getJson<any[]>(`${API_BASE}/admin/policy/matches?limit=25`));
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

        <div style={{ marginTop: 10 }}>
          <SummaryCards
            cards={[
              {
                label: "Active model",
                value: info?.registryVersion ?? "none",
                hint: "Current registryVersion used by the decision engine",
              },
              {
                label: "Format",
                value: info?.registryFormat ?? "—",
                hint: "Model serialization format",
              },
              {
                label: "Last trained",
                value: info?.lastTrainedAt ? new Date(info.lastTrainedAt).toLocaleString() : "—",
                hint: "When the active model was last produced",
              },
              {
                label: "Canary",
                value: canaryText,
                hint: "Traffic split for canary model (if enabled)",
              },
            ]}
          />
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
        <SummaryCards
          cards={[
            {
              label: "Models",
              value: registry.length,
              hint: "Models stored in model_registry",
            },
            {
              label: "Active",
              value: registry.find((m: any) => m.active)?.version ?? "none",
              hint: "Currently active registry version",
            },
            {
              label: "Latest",
              value: registry[0]?.version ?? "—",
              hint: "Most recent registry entry (by query order)",
            },
            {
              label: "Actions",
              value: busy ? "Working…" : "Ready",
              hint: "Activate promotes a model to active=true",
            },
          ]}
        />

        <div style={{ marginTop: 12 }}>
          <ExpandablePanel title="Registry entries" hint="Promote a model to active" defaultOpen={false}>
            <div className="tableWrap">
              <table className="table">
                <thead>
                  <tr>
                    <th>id</th>
                    <th>created</th>
                    <th>version</th>
                    <th>format</th>
                    <th>active</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  {registry.map((m: any) => (
                    <tr key={m.id}>
                      <td className="mono">{m.id}</td>
                      <td className="muted">{m.createdAt}</td>
                      <td className="mono">{m.version}</td>
                      <td>{m.format}</td>
                      <td>{String(m.active)}</td>
                      <td>
                        <button className="btn" onClick={() => activateModel(m.id)} disabled={busy || m.active}>
                          Activate
                        </button>
                      </td>
                    </tr>
                  ))}
                  {registry.length === 0 && (
                    <tr><td colSpan={6} className="muted">No models yet (run retrain).</td></tr>
                  )}
                </tbody>
              </table>
            </div>
          </ExpandablePanel>
        </div>
      </div>

      <div style={sectionStyle}>
        <h3 style={{ marginTop: 0 }}>Scorecards</h3>
        <SummaryCards
          cards={[
            {
              label: "Scorecards",
              value: scorecards.length,
              hint: "Retrain KPI snapshots (baseline vs recovery)",
            },
            {
              label: "Latest",
              value: scorecards[0]?.createdAt ? new Date(scorecards[0].createdAt).toLocaleString() : "—",
              hint: "Most recent scorecard (by query order)",
            },
            {
              label: "Selected",
              value: selected ? `#${selected.id}` : "—",
              hint: "Open a scorecard to see the metrics",
            },
            {
              label: "Status",
              value: selected?.status ?? "—",
              hint: "Scorecard status",
              danger: selected?.status ? String(selected.status).toLowerCase().includes("fail") : false,
            },
          ]}
        />

        <div style={{ marginTop: 12 }}>
          <ExpandablePanel title="Scorecard list" hint="Open a scorecard to view baseline/recovery metrics" defaultOpen={false}>
            <div className="tableWrap">
              <table className="table">
                <thead>
                  <tr>
                    <th>id</th>
                    <th>created</th>
                    <th>trigger</th>
                    <th>modelId</th>
                    <th>status</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  {scorecards.map((s: any) => (
                    <tr key={s.id}>
                      <td className="mono">{s.id}</td>
                      <td className="muted">{s.createdAt}</td>
                      <td>{s.triggerType}</td>
                      <td className="mono">{s.modelId ?? ""}</td>
                      <td>{s.status}</td>
                      <td>
                        <button className="btn" onClick={() => openScorecard(s.id)} disabled={busy}>Open</button>
                      </td>
                    </tr>
                  ))}
                  {scorecards.length === 0 && (
                    <tr><td colSpan={6} className="muted">No scorecards yet.</td></tr>
                  )}
                </tbody>
              </table>
            </div>
          </ExpandablePanel>
        </div>

        {selected && (
          <div style={{ marginTop: 12 }}>
            <ExpandablePanel
              title={`Scorecard #${selected.id} (${selected.status})`}
              hint="Metrics are available, but raw JSON is opt-in"
              defaultOpen
            >
              <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr 1fr", gap: 12 }}>
                <JsonOptIn title="Baseline metrics" value={selected.baselineMetricsJson} />
                <JsonOptIn title="Recovery metrics" value={selected.recoveryMetricsJson} />
                <JsonOptIn title="Delta metrics" value={selected.deltaMetricsJson} />
              </div>
            </ExpandablePanel>
          </div>
        )}
      </div>

      <div style={sectionStyle}>
        <h3 style={{ marginTop: 0 }}>Change history</h3>
        <SummaryCards
          cards={[
            { label: "Events", value: changes.length, hint: "Model changes, canary updates, promotions, rollbacks" },
            { label: "Latest", value: changes[0]?.createdAt ? new Date(changes[0].createdAt).toLocaleString() : "—", hint: "Most recent change" },
            { label: "Last event", value: changes[0]?.eventType ?? "—", hint: "Most recent event type" },
            { label: "State", value: busy ? "Working…" : "Ready", hint: "UI action state" },
          ]}
        />
        <div style={{ marginTop: 12 }}>
          <ExpandablePanel title="Event log" hint="Full change log (expand to view)" defaultOpen={false}>
            <div className="tableWrap">
              <table className="table">
                <thead>
                  <tr>
                    <th>created</th>
                    <th>event</th>
                    <th>from</th>
                    <th>to</th>
                    <th>reason</th>
                  </tr>
                </thead>
                <tbody>
                  {changes.map((c: any, idx: number) => (
                    <tr key={idx}>
                      <td className="muted">{c.createdAt}</td>
                      <td>{c.eventType}</td>
                      <td className="mono">{c.fromModelId ?? ""}</td>
                      <td className="mono">{c.toModelId ?? ""}</td>
                      <td className="muted">{c.reason ?? ""}</td>
                    </tr>
                  ))}
                  {changes.length === 0 && (
                    <tr><td colSpan={5} className="muted">No change events yet.</td></tr>
                  )}
                </tbody>
              </table>
            </div>
          </ExpandablePanel>
        </div>
      </div>


      <div style={sectionStyle}>
        <h3 style={{ marginTop: 0 }}>Recent policy matches</h3>
        <div className="muted" style={{ marginBottom: 10 }}>
          Sessions where a policy rule matched and influenced (or could have influenced) the final decision.
        </div>
        <SummaryCards
          cards={[
            { label: "Matches", value: policyMatches.length, hint: "Recent sessions where a policy rule matched" },
            { label: "Latest", value: policyMatches[0]?.occurred_at ? new Date(policyMatches[0].occurred_at).toLocaleString() : "—", hint: "Most recent match" },
            { label: "Decision", value: policyMatches[0]?.decision ?? "—", hint: "Most recent decision" },
            { label: "Policy", value: policyMatches[0]?.policy?.policy_id ?? policyMatches[0]?.policy?.policyId ?? "—", hint: "Most recent policy id" },
          ]}
        />
        <div style={{ marginTop: 12 }}>
          <ExpandablePanel title="Match log" hint="Expand to view the full list" defaultOpen={false}>
            <div className="tableWrap">
              <table className="table">
                <thead>
                  <tr>
                    <th>time</th>
                    <th>session</th>
                    <th>user</th>
                    <th>decision</th>
                    <th>confidence</th>
                    <th>policy</th>
                    <th>reason</th>
                  </tr>
                </thead>
                <tbody>
                  {policyMatches.map((p: any, idx: number) => (
                    <tr key={idx}>
                      <td className="muted">{p.occurred_at ?? ""}</td>
                      <td className="mono">{p.session_id ?? ""}</td>
                      <td className="mono">{p.user_id ?? ""}</td>
                      <td>{p.decision ?? ""}</td>
                      <td className="mono">{typeof p.confidence === "number" ? p.confidence.toFixed(3) : p.confidence ?? ""}</td>
                      <td className="mono">{p.policy?.policy_id ?? p.policy?.policyId ?? ""}</td>
                      <td className="muted">{p.policy?.reason ?? p.policy?.description ?? ""}</td>
                    </tr>
                  ))}
                  {policyMatches.length === 0 && (
                    <tr><td colSpan={7} className="muted">No policy matches yet.</td></tr>
                  )}
                </tbody>
              </table>
            </div>
          </ExpandablePanel>
        </div>
      </div>
      <div style={sectionStyle}>
        <h3 style={{ marginTop: 0 }}>Retrain jobs</h3>
        <SummaryCards
          cards={[
            { label: "Jobs", value: jobs.length, hint: "Retrain job queue" },
            { label: "Latest", value: jobs[0]?.createdAt ? new Date(jobs[0].createdAt).toLocaleString() : "—", hint: "Most recent job" },
            { label: "Latest status", value: jobs[0]?.status ?? "—", hint: "Most recent status" },
            { label: "State", value: busy ? "Working…" : "Ready", hint: "UI action state" },
          ]}
        />
        <div style={{ marginTop: 12 }}>
          <ExpandablePanel title="Job list" hint="Expand to view full job history" defaultOpen={false}>
            <div className="tableWrap">
              <table className="table">
                <thead>
                  <tr>
                    <th>id</th>
                    <th>created</th>
                    <th>status</th>
                    <th>from</th>
                    <th>to</th>
                    <th>reason</th>
                    <th>error</th>
                  </tr>
                </thead>
                <tbody>
                  {jobs.map((j: any) => (
                    <tr key={j.id}>
                      <td className="mono">{j.id}</td>
                      <td className="muted">{j.createdAt}</td>
                      <td>{j.status}</td>
                      <td className="mono">{j.fromModelId ?? ""}</td>
                      <td className="mono">{j.toModelId ?? ""}</td>
                      <td className="muted">{j.reason ?? ""}</td>
                      <td className="muted">{j.error ?? ""}</td>
                    </tr>
                  ))}
                  {jobs.length === 0 && (
                    <tr><td colSpan={7} className="muted">No jobs yet.</td></tr>
                  )}
                </tbody>
              </table>
            </div>
          </ExpandablePanel>
        </div>
      </div>
    </div>
  );
}
