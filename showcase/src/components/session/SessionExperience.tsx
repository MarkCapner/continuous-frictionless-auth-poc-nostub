
import { SessionRiskHeader } from "./header/SessionRiskHeader";
import { SessionNarrative } from "./narrative/SessionNarrative";
import { EvidenceCard } from "./cards/EvidenceCard";

export function SessionExperience({ session }: any) {
  return (
    <>
      <SessionRiskHeader
        risk={session.risk}
        decision={session.decision}
        confidence={session.mlConfidence}
      />

      <SessionNarrative text={session.narrative} />

      <EvidenceCard title="Device" summary="Minor change detected">
        Device fingerprint slightly different than baseline.
      </EvidenceCard>

      <EvidenceCard title="Behaviour" summary="Typing speed anomaly">
        Keystroke timing deviated from normal pattern.
      </EvidenceCard>

      <EvidenceCard title="TLS" summary="Known family">
        TLS fingerprint matches an existing trusted family.
      </EvidenceCard>
    </>
  );
}
