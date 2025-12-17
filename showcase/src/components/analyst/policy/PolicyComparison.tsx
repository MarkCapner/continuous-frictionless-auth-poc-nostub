
export function PolicyComparison({ actual, shadow }: any) {
  return (
    <div style={{ display: "flex", gap: 16 }}>
      <div>
        <h4>Actual Decision</h4>
        <strong>{actual}</strong>
      </div>
      <div>
        <h4>Shadow ML Policy</h4>
        <strong>{shadow}</strong>
      </div>
    </div>
  );
}
