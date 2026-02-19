import React from "react";
import WalletInput from "./components/WalletInput";

export default function App() {
  return (
    <div className="page">
      <div className="card">
        <h1>ChainTrust Wallet Risk Analyzer</h1>
        <p className="subtitle">
          Analyze wallet activity, score risk via ML, and generate a loan decision hash.
        </p>
        <WalletInput />
      </div>
    </div>
  );
}
