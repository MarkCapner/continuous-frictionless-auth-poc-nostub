
import React, { useEffect, useState } from "react";
import { DomainContributionCard } from "../../components/explainability/DomainContributionCard";
import { ExplanationNarrative } from "../../components/explainability/ExplanationNarrative";
import { AdminExplainabilityTrendsView } from "./AdminExplainabilityTrendsView";

export function AdminSessionExplainabilityView({ sessionId, userId }:{sessionId:string, userId:string}) {
  const [data,setData]=useState<any>(null);
  const [selectedDomain,setSelectedDomain]=useState<string|null>(null);

  useEffect(()=>{
    fetch(`/api/admin/explainability/session/${sessionId}`)
      .then(r=>r.json()).then(setData);
  },[sessionId]);

  if(!data) return <div>Loading explainability…</div>;
  if(!data.domains || data.domains.length===0)
    return <div>No explainability data available for this session.</div>;

  const top=data.domains[0];

  return (
    <div>
      <h2>Session Explainability</h2>
      <ExplanationNarrative primaryDomain={top.domain} label={top.baseline.label}/>
      <div style={{display:"flex",gap:16}}>
        {data.domains.map((d:any)=>(
          <div key={d.domain} onClick={()=>setSelectedDomain(d.domain)}>
            <DomainContributionCard
              domain={d.domain}
              score={d.score}
              label={d.baseline.label}
            />
          </div>
        ))}
      </div>

      {selectedDomain && (
        <div style={{marginTop:24}}>
          <AdminExplainabilityTrendsView userId={userId} domain={selectedDomain}/>
        </div>
      )}
    </div>
  );
}
