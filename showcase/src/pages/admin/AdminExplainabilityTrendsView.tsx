
import React, { useEffect, useState } from "react";
import { DomainTrendChart } from "../../components/explainability/DomainTrendChart";

export function AdminExplainabilityTrendsView({ userId, domain }:{userId:string, domain:string}) {
  const [data,setData]=useState<any[]>([]);

  useEffect(()=>{
    fetch(`/api/admin/explainability/trends?userId=${userId}&domain=${domain}`)
      .then(r=>r.json()).then(setData);
  },[userId,domain]);

  return (
    <div>
      <h2>{domain} Contribution Trends</h2>
      <DomainTrendChart data={data}/>
    </div>
  );
}
