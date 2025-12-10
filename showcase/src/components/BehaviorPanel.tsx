import type { BehaviorTelemetry } from "../api";

interface BehaviorPanelProps {
  behavior: BehaviorTelemetry | null;
}

export function BehaviorPanel({ behavior }: BehaviorPanelProps) {
  if (!behavior) {
    return (
      <div style={cardStyle}>
        <h2>Behavior</h2>
        <p>No behavior captured yet. Move the mouse, scroll and type before running a check.</p>
        <p style={noteStyle}>Privacy: only aggregate counts and timings are collected, never raw keystrokes.</p>
      </div>
    );
  }

  const metrics = [
    { key: "mouse_moves", label: "Mouse moves", value: behavior.mouse_moves },
    { key: "mouse_distance", label: "Mouse distance (px)", value: behavior.mouse_distance },
    { key: "key_presses", label: "Key presses", value: behavior.key_presses },
    { key: "avg_key_interval_ms", label: "Avg key interval (ms)", value: behavior.avg_key_interval_ms },
    { key: "scroll_events", label: "Scroll events", value: behavior.scroll_events }
  ];

  const max = Math.max(...metrics.map(m => m.value || 0), 1);

  return (
    <div style={cardStyle}>
      <h2>Behavior</h2>
      <div style={{ display: "flex", flexDirection: "column", gap: "0.5rem" }}>
        {metrics.map(m => (
          <div key={m.key}>
            <div style={{ display: "flex", justifyContent: "space-between" }}>
              <span>{m.label}</span>
              <span>{m.value.toFixed ? m.value.toFixed(0) : m.value}</span>
            </div>
            <svg width="100%" height="10">
              <rect
                x={0}
                y={0}
                width={`${(m.value / max) * 100}%`}
                height={10}
                style={{ fill: "#4f46e5" }}
              />
            </svg>
          </div>
        ))}
      </div>
      <p style={noteStyle}>Privacy: only aggregate counts and timings are captured, never key contents.</p>
    </div>
  );
}

const cardStyle = {
  border: "1px solid #ddd",
  borderRadius: 8,
  padding: "1rem",
  background: "#fff",
  boxShadow: "0 1px 3px rgba(0,0,0,0.05)",
  flex: 1,
  minWidth: 260
};

const noteStyle = {
  fontSize: "0.8rem",
  color: "#555",
  marginTop: "0.75rem"
};
