import React, { useState, useMemo, useCallback } from "react";
import axios from "axios";
import { Search, Loader, AlertCircle, ChevronDown, ChevronUp } from "lucide-react";
import RiskGauge from "../components/RiskGauge";
import FeatureRadar from "../components/FeatureRadar";

const BACKEND = process.env.REACT_APP_BACKEND_URL || "http://localhost:8080";
const ETH_RE = /^0x[a-fA-F0-9]{40}$/;

const FEATURE_DISPLAY = [
    { key: "walletAgeDays", label: "Wallet Age", unit: "days", good: "high" },
    { key: "txCount", label: "Tx Count", unit: "", good: "high" },
    { key: "avgTxValue", label: "Avg Tx Value", unit: "ETH", good: "low" },
    { key: "uniqueContracts", label: "Unique Contracts", unit: "", good: "high" },
    { key: "incomingOutgoingRatio", label: "I/O Ratio", unit: "", good: "high" },
    { key: "txVariance", label: "Tx Variance", unit: "", good: "low" },
    { key: "defiProtocolCount", label: "DeFi Protocols", unit: "", good: "high" },
    { key: "flashLoanCount", label: "Flash Loans", unit: "", good: "low" },
    { key: "liquidationEvents", label: "Liquidations", unit: "", good: "low" },
    { key: "nftTransactionCount", label: "NFT Txns", unit: "", good: "high" },
    { key: "maxSingleTxEth", label: "Max Single Tx", unit: "ETH", good: "none" },
    { key: "dormantPeriodDays", label: "Max Dormancy", unit: "days", good: "low" },
    { key: "collateralRatio", label: "Collateral Ratio", unit: "x", good: "high" },
    { key: "crossChainCount", label: "Cross-Chain Txns", unit: "", good: "high" },
    { key: "rugpullExposureScore", label: "Rugpull Exposure", unit: "%", good: "low" },
];

export default function AnalyzePage({ onOpenLogin, currentUser }) {
    const [address, setAddress] = useState("");
    const [features, setFeatures] = useState(null);
    const [risk, setRisk] = useState(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");
    const [showRaw, setShowRaw] = useState(false);

    async function analyze() {
        const addr = address.trim();
        if (!ETH_RE.test(addr)) { setError("Enter a valid EVM wallet address (0x…40 hex chars)"); return; }
        setError(""); setFeatures(null); setRisk(null); setLoading(true);
        try {
            const [fResp, rResp] = await Promise.all([
                axios.get(`${BACKEND}/wallet/${addr}`),
                axios.get(`${BACKEND}/wallet/${addr}`).then(r =>
                    axios.post(`${BACKEND}/risk`, r.data)
                ),
            ]);
            setFeatures(fResp.data);
            setRisk(rResp.data);
        } catch (err) {
            setError(err?.response?.data?.message || err?.response?.data?.detail || err.message || "Request failed");
        } finally {
            setLoading(false);
        }
    }

    function handleKey(e) { if (e.key === "Enter") analyze(); }

    const riskScore = risk ? Number(risk.riskScore ?? 0) : 0;
    const riskLevel = risk?.riskLevel ?? "LOW";

    return (
        <div className="section">
            <div className="container">
                {/* Header */}
                <div style={{ marginBottom: 32 }}>
                    <span className="badge badge-info" style={{ marginBottom: 12 }}>Wallet Analyzer</span>
                    <h1 className="page-title" style={{ fontSize: 34 }}>
                        Analyze <span className="gradient-text">Wallet Risk</span>
                    </h1>
                    <p className="section-subtitle" style={{ marginTop: 8, fontSize: 15 }}>
                        Enter any EVM address to get a full DeFi credit profile powered by 15 on-chain features.
                    </p>
                    {!currentUser && (
                        <div style={{ marginTop: 14 }}>
                            <button type="button" className="btn btn-ghost btn-sm" onClick={onOpenLogin}>
                                Login to save your profile
                            </button>
                        </div>
                    )}
                </div>

                {/* Search bar */}
                <div className="glass-card" style={{ padding: 24, marginBottom: 28 }}>
                    <div className="input-group">
                        <input
                            className="ct-input"
                            value={address}
                            onChange={(e) => setAddress(e.target.value)}
                            onKeyDown={handleKey}
                            placeholder="0x… Ethereum wallet address"
                        />
                        <button className="btn btn-primary" onClick={analyze} disabled={loading}>
                            {loading ? <><div className="spinner" style={{ width: 16, height: 16, borderWidth: 2 }} /> Analyzing…</> : <><Search size={15} /> Analyze</>}
                        </button>
                    </div>
                    {error && (
                        <div className="alert alert-error" style={{ marginTop: 12 }}>
                            <AlertCircle size={16} /> {error}
                        </div>
                    )}
                </div>

                {/* Results */}
                {risk && features && (
                    <div className="grid-2" style={{ alignItems: "start" }}>

                        {/* Left — gauge + profiles */}
                        <div style={{ display: "flex", flexDirection: "column", gap: 20 }}>

                            {/* Risk Gauge */}
                            <div className="glass-card" style={{ padding: 28, textAlign: "center" }}>
                                <div className="label-sm" style={{ marginBottom: 16 }}>Real-Time Risk Score</div>
                                <RiskGauge riskScore={riskScore} riskLevel={riskLevel} />
                                <div className="divider" />
                                <div style={{ display: "flex", justifyContent: "space-between", fontSize: 13 }}>
                                    <span style={{ color: "var(--text-secondary)" }}>First Seen</span>
                                    <span style={{ fontFamily: "var(--font-mono)", fontSize: 12 }}>{features.firstSeenDate || "N/A"}</span>
                                </div>
                                <div style={{ display: "flex", justifyContent: "space-between", fontSize: 13, marginTop: 8 }}>
                                    <span style={{ color: "var(--text-secondary)" }}>Total Volume</span>
                                    <span><strong>{(features.totalVolumeEth || 0).toFixed(3)}</strong> ETH</span>
                                </div>
                            </div>

                            {/* Feature Radar */}
                            <div className="glass-card" style={{ padding: 24 }}>
                                <div className="section-title" style={{ marginBottom: 16 }}>DeFi Feature Profile</div>
                                <FeatureRadar features={features} />
                            </div>
                        </div>

                        {/* Right — feature table */}
                        <div style={{ display: "flex", flexDirection: "column", gap: 20 }}>

                            {/* Known protocols */}
                            {features.knownProtocols?.length > 0 && (
                                <div className="glass-card" style={{ padding: 20 }}>
                                    <div className="section-title" style={{ marginBottom: 12 }}>Known DeFi Protocols</div>
                                    <div style={{ display: "flex", flexWrap: "wrap", gap: 8 }}>
                                        {features.knownProtocols.map((p) => (
                                            <span key={p} className="badge badge-cyan">{p}</span>
                                        ))}
                                    </div>
                                </div>
                            )}

                            {/* Feature breakdown */}
                            <div className="glass-card" style={{ padding: 20 }}>
                                <div className="section-title" style={{ marginBottom: 16 }}>Feature Breakdown</div>
                                <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
                                    {FEATURE_DISPLAY.map(({ key, label, unit, good }) => {
                                        const raw = features[key] ?? 0;
                                        const val = unit === "%" ? (raw * 100).toFixed(1) : typeof raw === "number" ? raw.toFixed(raw < 10 ? 3 : 0) : raw;
                                        const color = good === "high"
                                            ? (raw > 0 ? "var(--success)" : "var(--warning)")
                                            : good === "low"
                                                ? (raw > 0 ? "var(--danger)" : "var(--success)")
                                                : "var(--text-primary)";
                                        const isRisk = good === "low" && raw > 0;
                                        return (
                                            <div key={key} style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                                                <span style={{ fontSize: 13, color: "var(--text-secondary)" }}>{label}</span>
                                                <span style={{ fontSize: 13, fontWeight: 600, color, fontFamily: unit === "" && raw > 100 ? "var(--font-mono)" : "inherit" }}>
                                                    {val}{unit && ` ${unit}`}
                                                    {isRisk && <span style={{ marginLeft: 6, fontSize: 10 }} className="badge badge-danger">⚠</span>}
                                                </span>
                                            </div>
                                        );
                                    })}
                                </div>
                            </div>

                            {/* ML contribution */}
                            {risk.featureContributions && (
                                <div className="glass-card" style={{ padding: 20 }}>
                                    <div className="section-title" style={{ marginBottom: 14 }}>ML Sensitivity (top factors)</div>
                                    {Object.entries(risk.featureContributions)
                                        .sort((a, b) => Math.abs(b[1]) - Math.abs(a[1]))
                                        .slice(0, 7)
                                        .map(([k, v]) => (
                                            <div key={k} style={{ marginBottom: 10 }}>
                                                <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 4 }}>
                                                    <span style={{ fontSize: 12, color: "var(--text-secondary)" }}>{k.replace(/_/g, " ")}</span>
                                                    <span style={{ fontSize: 12, color: v > 0 ? "var(--success)" : "var(--danger)" }}>
                                                        {v > 0 ? "+" : ""}{(v * 100).toFixed(2)}
                                                    </span>
                                                </div>
                                                <div className="progress-bar-wrap">
                                                    <div className="progress-bar-fill" style={{
                                                        width: `${Math.min(Math.abs(v) * 1000, 100)}%`,
                                                        background: v > 0 ? "linear-gradient(90deg,#10b981,#34d399)" : "linear-gradient(90deg,#ef4444,#f87171)"
                                                    }} />
                                                </div>
                                            </div>
                                        ))}
                                </div>
                            )}

                            {/* Raw JSON */}
                            <div className="glass-card" style={{ padding: 16 }}>
                                <button
                                    onClick={() => setShowRaw(!showRaw)}
                                    className="btn btn-ghost btn-sm"
                                    style={{ width: "100%", justifyContent: "space-between" }}
                                >
                                    <span>Raw Response</span>
                                    {showRaw ? <ChevronUp size={14} /> : <ChevronDown size={14} />}
                                </button>
                                {showRaw && (
                                    <pre style={{
                                        marginTop: 12, fontSize: 11, background: "rgba(0,0,0,0.3)",
                                        borderRadius: 8, padding: 12, color: "var(--accent)",
                                        fontFamily: "var(--font-mono)", overflowX: "auto"
                                    }}>
                                        {JSON.stringify({ features, risk }, null, 2)}
                                    </pre>
                                )}
                            </div>
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
}
