import { useState } from "react";
import { setChaosHighRiskAction, setChaosNightTime, setChaosVpn } from "../profiler";

export function ChaosToggles() {
  const [vpn, setVpn] = useState(false);
  const [highRisk, setHighRisk] = useState(false);
  const [night, setNight] = useState(false);

  const onVpnChange = (checked: boolean) => {
    setVpn(checked);
    setChaosVpn(checked);
  };

  const onHighRiskChange = (checked: boolean) => {
    setHighRisk(checked);
    setChaosHighRiskAction(checked);
  };

  const onNightChange = (checked: boolean) => {
    setNight(checked);
    setChaosNightTime(checked);
  };

  return (
    <div style={cardStyle}>
      <h2>Chaos toggles</h2>
      <p style={{ fontSize: "0.85rem", color: "#555" }}>
        Simulate riskier conditions without changing backend code. These flags feed into the context sent to the API.
      </p>
      <label style={labelStyle}>
        <input
          type="checkbox"
          checked={vpn}
          onChange={e => onVpnChange(e.target.checked)}
        />
        Simulate VPN / proxy (context.vpn = true)
      </label>
      <label style={labelStyle}>
        <input
          type="checkbox"
          checked={highRisk}
          onChange={e => onHighRiskChange(e.target.checked)}
        />
        Simulate high-risk action (context.high_risk_action = true)
      </label>
      <label style={labelStyle}>
        <input
          type="checkbox"
          checked={night}
          onChange={e => onNightChange(e.target.checked)}
        />
        Simulate night-time login (hour = 02:00)
      </label>
    </div>
  );
}

const cardStyle = {
  border: "1px solid #ddd",
  borderRadius: 8,
  padding: "1rem",
  background: "var(--panel)",
  boxShadow: "0 1px 3px rgba(0,0,0,0.05)",
  minWidth: 260
};

const labelStyle = {
  display: "block",
  marginTop: "0.5rem",
  fontSize: "0.9rem"
};
