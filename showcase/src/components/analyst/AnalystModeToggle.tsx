
export function AnalystModeToggle({ enabled, onToggle }: any) {
  return (
    <label style={{ display: "flex", gap: 8 }}>
      <input
        type="checkbox"
        checked={enabled}
        onChange={e => onToggle(e.target.checked)}
      />
      Analyst Mode
    </label>
  );
}
