import React from "react";

export default function RiskDashboard({ features, risk, loanResult }) {
  const score = Number(risk?.riskScore ?? 0);
  const pct = (score * 100).toFixed(1);

  return (
    <div className="result-card">
      <h2>
        Risk: {risk?.riskLevel} ({pct}%)
      </h2>
      <div className="pill-row">
        <span className={`pill ${risk?.riskLevel === "HIGH" ? "pill-high" : risk?.riskLevel === "MEDIUM" ? "pill-medium" : "pill-low"}`}>
          {risk?.riskLevel}
        </span>
        {loanResult && (
          <span className={`pill ${loanResult.approved ? "pill-low" : "pill-high"}`}>
            Loan {loanResult.approved ? "APPROVED" : "DENIED"}
          </span>
        )}
      </div>

      <h3>Wallet Features</h3>
      <pre>{JSON.stringify(features, null, 2)}</pre>

      {loanResult && (
        <>
          <h3>Loan Decision</h3>
          <pre>{JSON.stringify(loanResult, null, 2)}</pre>
        </>
      )}
    </div>
  );
}
