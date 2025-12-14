import React from "react";
import type { BehaviorTelemetry } from "../api";

interface BehaviorPanelProps {
  behavior: BehaviorTelemetry | null;
}

export function BehaviorPanel({ behavior }: BehaviorPanelProps) {
  return (
    <div className="card">
      <div className="cardTitle">
        <h3>Behaviour signals</h3>
        <span className="chip chipAccent2">privacy-safe</span>
      </div>

      {!behavior ? (
        <>
          <p className="muted" style={{ marginTop: 0 }}>
            No behaviour captured yet. Move the mouse, scroll, and type before running a check.
          </p>
          <p className="muted" style={{ fontSize: 12, marginTop: 10 }}>
            Privacy: only aggregate counts and timings are collected — never raw keystrokes.
          </p>
        </>
      ) : (
        <>
          <p className="muted" style={{ fontSize: 13, marginTop: 0 }}>
            These features come from the built-in behavioural profiler. Type and move the mouse before clicking{" "}
            <strong>Run profile check</strong> to update them.
          </p>
          <div style={{ display: "flex", flexWrap: "wrap", gap: 8 }}>
            <span className="chip">mouse moves · {behavior.mouse_moves}</span>
            <span className="chip">mouse distance · {behavior.mouse_distance.toFixed(1)} px</span>
            <span className="chip">key presses · {behavior.key_presses}</span>
            <span className="chip">avg key interval · {behavior.avg_key_interval_ms.toFixed(1)} ms</span>
            <span className="chip">key interval std · {behavior.key_interval_std_ms.toFixed(1)} ms</span>
            <span className="chip">scroll events · {behavior.scroll_events}</span>
            <span className="chip">scroll cadence · {behavior.scroll_events_per_sec.toFixed(2)}/s</span>
            <span className="chip">pointer avg v · {behavior.pointer_avg_velocity.toFixed(4)} px/ms</span>
            <span className="chip">pointer max v · {behavior.pointer_max_velocity.toFixed(4)} px/ms</span>
          </div>
        </>
      )}

      <div className="divider" />

      <label style={{ display: "block" }}>
        <div className="muted" style={{ fontSize: 12, marginBottom: 6 }}>
          Typing playground (for keystroke dynamics)
        </div>
        <textarea
          rows={3}
          placeholder="Type here at your normal speed. Only timing statistics are sent, never the raw text."
          style={{
            width: "100%",
            borderRadius: 12,
            border: "1px solid rgba(255,255,255,0.14)",
            background: "rgba(255,255,255,0.05)",
            color: "var(--text)",
            padding: "10px",
            fontSize: 13,
            resize: "vertical",
            boxSizing: "border-box",
            outline: "none"
          }}
        />
      </label>
      <p className="muted" style={{ fontSize: 12, marginTop: 10, marginBottom: 0 }}>
        Tip: try different typing styles or devices (e.g. laptop vs external keyboard) and see how the behavioural score
        and overall decision change.
      </p>
    </div>
  );
}
