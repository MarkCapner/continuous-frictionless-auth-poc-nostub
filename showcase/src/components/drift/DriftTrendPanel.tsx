
export function DriftTrendPanel({ trend }: any) {
  return (
    <div>
      <h3>Drift Trend</h3>
      <p>Status: {trend.trend}</p>
      <p>Start Drift: {trend.startDrift}</p>
      <p>End Drift: {trend.endDrift}</p>
    </div>
  );
}
