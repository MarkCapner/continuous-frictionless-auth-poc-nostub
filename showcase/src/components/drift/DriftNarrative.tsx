
export function DriftNarrative({ narrative }: any) {
  return (
    <div>
      <h3>Drift Explanation</h3>
      <p><strong>Dominant Signal:</strong> {narrative.dominantSignal}</p>
      <p>{narrative.summary}</p>
    </div>
  );
}
