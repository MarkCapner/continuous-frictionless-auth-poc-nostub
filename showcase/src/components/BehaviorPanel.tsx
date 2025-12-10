import React from "react";
import type { BehaviorTelemetry } from "../api";

interface BehaviorPanelProps {
  behavior: BehaviorTelemetry | null;
}

export function BehaviorPanel({ behavior }: BehaviorPanelProps) {
  return (
    <div style={cardStyle}>
      <h2>Behavior</h2>
      {!behavior && (
        <>
          <p>No behavior captured yet. Move the mouse, scroll and type before running a check.</p>
          <p style={noteStyle}>
            Privacy: only aggregate counts and timings are collected, never raw keystrokes.
          </p>
        </>
      )}
      {behavior && (
        <>
          <p style={{ fontSize: "0.9rem", marginTop: 0 }}>
            These features come from the built-in behavioural profiler. Type and move the mouse before
            clicking <strong>Run profile check</strong> to update them.
          </p>
          <ul style={{ paddingLeft: "1.1rem", fontSize: "0.9rem" }}>
            <li>Mouse moves: {behavior.mouse_moves}</li>
            <li>Mouse distance: {behavior.mouse_distance.toFixed(1)} px</li>
            <li>Key presses: {behavior.key_presses}</li>
            <li>Avg key interval: {behavior.avg_key_interval_ms.toFixed(1)} ms</li>
            <li>Scroll events: {behavior.scroll_events}</li>
          </ul>
        </>
      )}
      <div style={{ marginTop: "0.75rem" }}>
        <label style={{ fontSize: "0.85rem", fontWeight: 500, display: "block", marginBottom: "0.25rem" }}>
          Typing playground (for keystroke dynamics)
        </label>
        <textarea
          rows={3}
          placeholder="Type here at your normal speed. Only timing statistics are sent, never the raw text."
          style={textareaStyle}
        />
        <p style={noteStyle}>
          Tip: try different typing styles or devices (e.g. laptop vs. external keyboard) and see how the
          behavioural score and overall decision change.
        </p>
      </div>
    </div>
  );
}

const cardStyle: React.CSSProperties = {
  border: "1px solid #ddd",
  borderRadius: 8,
  padding: "1rem",
  background: "#fff",
  boxShadow: "0 1px 3px rgba(0,0,0,0.05)",
  flex: 1,
  minWidth: 260
};

const noteStyle: React.CSSProperties = {
  fontSize: "0.8rem",
  color: "#555",
  marginTop: "0.75rem"
};

const textareaStyle: React.CSSProperties = {
  width: "100%",
  borderRadius: 6,
  border: "1px solid #d1d5db",
  padding: "0.5rem",
  fontSize: "0.85rem",
  resize: "vertical",
  boxSizing: "border-box"
};
