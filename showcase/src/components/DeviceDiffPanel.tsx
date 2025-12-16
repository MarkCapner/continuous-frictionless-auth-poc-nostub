import React, { useEffect, useState } from "react";
import type { DeviceProfileSummary, DeviceDiffResponse } from "../api";
import { fetchDeviceHistory, fetchDeviceDiff } from "../api";
import { ExpandablePanel } from "../ui/ExpandablePanel";

export interface DeviceDiffPanelProps {
  userHint: string;
}

/**
 * Mini-Epic 8C: device diff viewer wiring.
 *
 * This component lets you pick two device profiles for the selected user and
 * visualises field-level differences using the /showcase/devices/diff API.
 */
export function DeviceDiffPanel({ userHint }: DeviceDiffPanelProps) {
  const [devices, setDevices] = useState<DeviceProfileSummary[]>([]);
  const [loadingDevices, setLoadingDevices] = useState(false);
  const [errorDevices, setErrorDevices] = useState<string | null>(null);

  const [leftId, setLeftId] = useState<number | null>(null);
  const [rightId, setRightId] = useState<number | null>(null);

  const [diff, setDiff] = useState<DeviceDiffResponse | null>(null);
  const [loadingDiff, setLoadingDiff] = useState(false);
  const [errorDiff, setErrorDiff] = useState<string | null>(null);

  // Fetch devices when user changes
  useEffect(() => {
    let cancelled = false;
    const run = async () => {
      const hint = (userHint || "demo-user").trim() || "demo-user";
      setLoadingDevices(true);
      setErrorDevices(null);
      setDiff(null);
      setLeftId(null);
      setRightId(null);
      try {
        const data = await fetchDeviceHistory(hint);
        if (!cancelled) {
          setDevices(data);
        }
      } catch (e: any) {
        if (!cancelled) {
          setErrorDevices(e?.message ?? String(e));
        }
      } finally {
        if (!cancelled) {
          setLoadingDevices(false);
        }
      }
    };
    void run();
    return () => {
      cancelled = true;
    };
  }, [userHint]);

  const canCompare = leftId != null && rightId != null && leftId !== rightId;

  const handleCompare = async () => {
    if (!canCompare || leftId == null || rightId == null) {
      return;
    }
    setLoadingDiff(true);
    setErrorDiff(null);
    try {
      const res = await fetchDeviceDiff(leftId, rightId);
      setDiff(res);
    } catch (e: any) {
      setErrorDiff(e?.message ?? String(e));
      setDiff(null);
    } finally {
      setLoadingDiff(false);
    }
  };

  return (
    <div style={cardStyle}>
      <h3 style={titleStyle}>Device diff viewer</h3>
      <p style={bodyStyle}>
        Compare two stored device profiles for <code>{(userHint || "demo-user").trim() || "demo-user"}</code>. This
        is useful for spotting subtle changes such as timezone, screen size, or WebGL/canvas drift.
      </p>
      {loadingDevices && <p style={bodyStyle}>Loading devices…</p>}
      {errorDevices && (
        <p style={{ ...bodyStyle, color: "#b91c1c" }}>
          Error while loading devices: {errorDevices}
        </p>
      )}
      {!loadingDevices && !errorDevices && devices.length === 0 && (
        <p style={placeholderStyle}>No devices recorded yet – run a few profile checks first.</p>
      )}
      {!loadingDevices && !errorDevices && devices.length > 0 && (
        <>
          <div style={selectorsRowStyle}>
            <div style={selectColumnStyle}>
              <label style={labelStyle}>
                Left device
                <select
                  value={leftId ?? ""}
                  onChange={(e) => setLeftId(e.target.value ? Number(e.target.value) : null)}
                  style={selectStyle}
                >
                  <option value="">(select)</option>
                  {devices.map((d) => (
                    <option key={d.id} value={d.id}>
                      #{d.id} · {d.uaFamily || "Unknown"} {d.uaVersion || ""} · {d.screenW}×{d.screenH}
                    </option>
                  ))}
                </select>
              </label>
            </div>
            <div style={selectColumnStyle}>
              <label style={labelStyle}>
                Right device
                <select
                  value={rightId ?? ""}
                  onChange={(e) => setRightId(e.target.value ? Number(e.target.value) : null)}
                  style={selectStyle}
                >
                  <option value="">(select)</option>
                  {devices.map((d) => (
                    <option key={d.id} value={d.id}>
                      #{d.id} · {d.uaFamily || "Unknown"} {d.uaVersion || ""} · {d.screenW}×{d.screenH}
                    </option>
                  ))}
                </select>
              </label>
            </div>
            <div style={buttonColumnStyle}>
              <button
                type="button"
                onClick={handleCompare}
                disabled={!canCompare || loadingDiff}
                style={buttonStyle}
              >
                {loadingDiff ? "Comparing…" : "Compare"}
              </button>
              {!canCompare && (
                <p style={hintStyle}>Pick two different devices to run a diff.</p>
              )}
            </div>
          </div>

          {errorDiff && (
            <p style={{ ...bodyStyle, color: "#b91c1c" }}>Error while diffing devices: {errorDiff}</p>
          )}

          {diff && (
            <div style={{ marginTop: "0.75rem" }}>
              <div style={summaryRowStyle}>
                <div style={summaryColumnStyle}>
                  <h4 style={summaryTitleStyle}>Left</h4>
                  <DeviceSummaryBlock device={diff.left} />
                </div>
                <div style={summaryColumnStyle}>
                  <h4 style={summaryTitleStyle}>Right</h4>
                  <DeviceSummaryBlock device={diff.right} />
                </div>
              </div>
              <h4 style={{ ...summaryTitleStyle, marginTop: "0.75rem" }}>Differences</h4>
              {diff.changes.length === 0 ? (
                <p style={bodyStyle}>No differences detected between these two device profiles.</p>
              ) : (
                <div style={{ marginTop: 8, display: "flex", flexDirection: "column", gap: 8 }}>
                  {diff.changes.slice(0, 10).map((c, idx) => (
                    <div key={idx} className="summaryCard" style={{ padding: "0.55rem 0.7rem" }}>
                      <div style={{ display: "flex", justifyContent: "space-between", gap: 10, alignItems: "center" }}>
                        <div style={{ fontWeight: 700 }}>{c.field}</div>
                        <span className="chip">changed</span>
                      </div>
                      <ExpandablePanel title="Show values" hint="left vs right" defaultOpen={false}>
                        <div style={{ display: "grid", gridTemplateColumns: "minmax(0, 1fr) minmax(0, 1fr)", gap: 10 }}>
                          <div>
                            <div className="muted" style={{ fontSize: 12, marginBottom: 6 }}>Left</div>
                            <div style={{ fontSize: "0.85rem", wordBreak: "break-word" }}>{String(c.leftValue ?? "")}</div>
                          </div>
                          <div>
                            <div className="muted" style={{ fontSize: 12, marginBottom: 6 }}>Right</div>
                            <div style={{ fontSize: "0.85rem", wordBreak: "break-word" }}>{String(c.rightValue ?? "")}</div>
                          </div>
                        </div>
                      </ExpandablePanel>
                    </div>
                  ))}

                  <ExpandablePanel title="Show full diff table" hint={`All fields (${diff.changes.length})`} defaultOpen={false}>
                    <div style={{ marginTop: "0.5rem" }}>
                      <table style={tableStyle}>
                        <thead>
                          <tr>
                            <th>Field</th>
                            <th>Left</th>
                            <th>Right</th>
                          </tr>
                        </thead>
                        <tbody>
                          {diff.changes.map((c, idx) => (
                            <tr key={idx}>
                              <td>{c.field}</td>
                              <td>{c.leftValue}</td>
                              <td>{c.rightValue}</td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  </ExpandablePanel>
                </div>
              )}
            </div>
          )}
        </>
      )}
    </div>
  );
}

interface DeviceSummaryBlockProps {
  device: DeviceProfileSummary;
}

function DeviceSummaryBlock({ device }: DeviceSummaryBlockProps) {
  return (
    <div style={summaryBlockStyle}>
      <div style={summaryLineStyle}>
        <strong>ID:</strong> #{device.id}
      </div>
      <div style={summaryLineStyle}>
        <strong>TLS FP:</strong> <code>{device.tlsFp}</code>
      </div>
      <div style={summaryLineStyle}>
        <strong>UA:</strong>{" "}
        {(device.uaFamily || "Unknown") + (device.uaVersion ? " " + device.uaVersion : "")}
      </div>
      <div style={summaryLineStyle}>
        <strong>Screen:</strong> {device.screenW}×{device.screenH} @ {device.pixelRatio}x
      </div>
      <div style={summaryLineStyle}>
        <strong>Timezone offset:</strong> {device.tzOffset}
      </div>
      <div style={summaryLineStyle}>
        <strong>Country:</strong> {device.lastCountry || "n/a"}
      </div>
      <div style={summaryLineStyle}>
        <strong>Seen count:</strong> {device.seenCount}
      </div>
    </div>
  );
}

const cardStyle: React.CSSProperties = {
  borderRadius: 8,
  borderWidth: 1,
  borderStyle: "solid",
  borderColor: "var(--border)",
  background: "var(--panel)",
  padding: "0.75rem 1rem",
  minHeight: 160,
  boxSizing: "border-box"
};

const titleStyle: React.CSSProperties = {
  margin: "0 0 0.35rem"
};

const bodyStyle: React.CSSProperties = {
  margin: "0.25rem 0"
};

const placeholderStyle: React.CSSProperties = {
  margin: "0.25rem 0 0",
  fontStyle: "italic",
  color: "#6b7280"
};

const selectorsRowStyle: React.CSSProperties = {
  display: "flex",
  flexWrap: "wrap",
  gap: "0.75rem",
  marginTop: "0.5rem",
  alignItems: "flex-end"
};

const selectColumnStyle: React.CSSProperties = {
  flex: "1 1 180px"
};

const labelStyle: React.CSSProperties = {
  display: "flex",
  flexDirection: "column",
  fontSize: "0.85rem",
  gap: "0.25rem"
};

const selectStyle: React.CSSProperties = {
  padding: "0.25rem 0.4rem",
  borderRadius: 6,
  borderWidth: 1,
  borderStyle: "solid",
  borderColor: "#d1d5db",
  fontSize: "0.85rem"
};

const buttonColumnStyle: React.CSSProperties = {
  display: "flex",
  flexDirection: "column",
  gap: "0.25rem"
};

const buttonStyle: React.CSSProperties = {
  padding: "0.35rem 0.9rem",
  borderRadius: 999,
  border: "none",
  cursor: "pointer",
  background: "#4f46e5",
  color: "var(--text)",
  fontSize: "0.85rem",
  fontWeight: 500,
  whiteSpace: "nowrap"
};

const hintStyle: React.CSSProperties = {
  margin: 0,
  fontSize: "0.75rem",
  color: "#6b7280"
};

const summaryRowStyle: React.CSSProperties = {
  display: "grid",
  gridTemplateColumns: "minmax(0, 1fr) minmax(0, 1fr)",
  gap: "0.75rem",
  marginTop: "0.75rem"
};

const summaryColumnStyle: React.CSSProperties = {
  minWidth: 0
};

const summaryTitleStyle: React.CSSProperties = {
  margin: "0 0 0.25rem",
  fontSize: "0.9rem"
};

const summaryBlockStyle: React.CSSProperties = {
  borderRadius: 8,
  borderWidth: 1,
  borderStyle: "solid",
  borderColor: "var(--border)",
  padding: "0.5rem 0.6rem",
  fontSize: "0.8rem",
  background: "rgba(255,255,255,0.04)"
};

const summaryLineStyle: React.CSSProperties = {
  margin: "0.1rem 0"
};

const tableStyle: React.CSSProperties = {
  width: "100%",
  borderCollapse: "collapse",
  fontSize: "0.8rem"
};
