
export function DriftTimeline({ summary }: any) {
  return (
    <div>
      <h3>Drift Timeline</h3>
      <pre>{JSON.stringify(summary, null, 2)}</pre>
    </div>
  );
}
