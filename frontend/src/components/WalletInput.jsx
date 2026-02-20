import React, { useMemo, useState } from "react";
import axios from "axios";
import RiskDashboard from "./RiskDashboard";

const ETH_ADDRESS_REGEX = /^0x[a-fA-F0-9]{40}$/;

export default function WalletInput() {
  const [address, setAddress] = useState("");
  const [features, setFeatures] = useState(null);
  const [risk, setRisk] = useState(null);
  const [loanResult, setLoanResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const backendUrl = useMemo(() => {
    return process.env.REACT_APP_BACKEND_URL || "http://localhost:8080";
  }, []);

  async function analyze() {
    setError("");
    setRisk(null);
    setLoanResult(null);

    if (!ETH_ADDRESS_REGEX.test(address.trim())) {
      setError("Enter a valid EVM wallet address.");
      return;
    }

    setLoading(true);
    try {
      const walletResp = await axios.get(`${backendUrl}/wallet/${address.trim()}`);
      setFeatures(walletResp.data);

      const riskResp = await axios.post(`${backendUrl}/risk`, walletResp.data);
      setRisk(riskResp.data);

      const loanResp = await axios.post(`${backendUrl}/loan/evaluate`, {
        walletAddress: address.trim(),
        amount: 1000
      });
      setLoanResult(loanResp.data);
    } catch (err) {
      const detail = err?.response?.data?.message || err?.response?.data?.detail || err.message;
      setError(detail || "Request failed");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div>
      <div className="row">
        <input
          className="input"
          value={address}
          onChange={(e) => setAddress(e.target.value)}
          placeholder="0x..."
        />
        <button className="button" onClick={analyze} disabled={loading}>
          {loading ? "Analyzing..." : "Analyze"}
        </button>
      </div>
      {error && <p className="error">{error}</p>}
      {risk && <RiskDashboard features={features} risk={risk} loanResult={loanResult} />}
    </div>
  );
}
