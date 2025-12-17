
import React from "react";

export function DomainTrendChart({ data }:{ data: Array<{day:string, avg_score:number, extreme_count:number}> }) {
  return (
    <table>
      <thead>
        <tr>
          <th>Date</th>
          <th>Avg Score</th>
          <th>Extreme Events</th>
        </tr>
      </thead>
      <tbody>
        {data.map((d,i)=>(
          <tr key={i}>
            <td>{d.day}</td>
            <td>{d.avg_score.toFixed(3)}</td>
            <td>{d.extreme_count}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
