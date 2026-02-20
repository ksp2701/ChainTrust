import React from "react";
import { Shield, Brain, Cpu, Code2, Database, Globe } from "lucide-react";

const STEPS = [
    {
        icon: <Globe size={28} />,
        num: "01",
        title: "Fetch On-Chain Data",
        desc: "ChainTrust fetches up to 100 real transactions for the wallet from Etherscan, or uses a deterministic realistic simulation if no API key is configured.",
        color: "var(--primary)",
    },
    {
        icon: <Brain size={28} />,
        num: "02",
        title: "Extract 15 DeFi Features",
        desc: "Wallet age, DeFi protocol usage, collateral ratio, flash loans, liquidation history, NFT tx count, rugpull exposure, dormancy, cross-chain activity and more.",
        color: "var(--accent)",
    },
    {
        icon: <Cpu size={28} />,
        num: "03",
        title: "ML Risk Scoring",
        desc: "A calibrated GradientBoosting + RandomForest ensemble (90%+ accuracy on 50,000 training samples) outputs a trust probability and risk level.",
        color: "var(--accent2)",
    },
    {
        icon: <Shield size={28} />,
        num: "04",
        title: "Credit Tier Assignment",
        desc: "The trust score maps to PLATINUM / GOLD / SILVER / BRONZE / REJECTED, each with appropriate interest rates (3.5%–14%) and loan limits.",
        color: "var(--success)",
    },
    {
        icon: <Database size={28} />,
        num: "05",
        title: "Reason Generation",
        desc: "Every decision is accompanied by a detailed list of contributing factors — what helped and what hurt the application.",
        color: "var(--warning)",
    },
    {
        icon: <Code2 size={28} />,
        num: "06",
        title: "On-Chain Proof",
        desc: "A SHA-256 hash of the decision is generated and can be stored immutably on the ChainTrust Ethereum smart contract.",
        color: "var(--danger)",
    },
];

const STACK = [
    { name: "React 18", role: "Frontend UI", color: "#61dafb" },
    { name: "React Router v6", role: "Multi-page routing", color: "var(--primary)" },
    { name: "Recharts", role: "Data visualization", color: "var(--accent2)" },
    { name: "Spring Boot 3", role: "Backend API", color: "#6db33f" },
    { name: "Python FastAPI", role: "ML inference service", color: "#009688" },
    { name: "scikit-learn", role: "ML model training", color: "#f7931e" },
    { name: "Etherscan API", role: "Real blockchain data", color: "var(--accent)" },
    { name: "Solidity 0.8", role: "Smart contract", color: "#627eea" },
];

export default function AboutPage() {
    return (
        <div className="section">
            <div className="container">
                {/* Header */}
                <div style={{ textAlign: "center", marginBottom: 56 }}>
                    <span className="badge badge-info" style={{ marginBottom: 16 }}>How It Works</span>
                    <h1 className="page-title" style={{ fontSize: 34 }}>
                        The <span className="gradient-text">ChainTrust</span> Pipeline
                    </h1>
                    <p className="section-subtitle" style={{ marginTop: 12, fontSize: 15, maxWidth: 560, margin: "12px auto 0" }}>
                        From a wallet address to a fully-reasoned DeFi credit decision in under 3 seconds.
                    </p>
                </div>

                {/* Steps */}
                <div className="grid-3" style={{ marginBottom: 56 }}>
                    {STEPS.map((s) => (
                        <div key={s.num} className="glass-card" style={{ padding: 28 }}>
                            <div style={{
                                width: 52, height: 52,
                                background: `rgba(${hexToRgb(s.color)},0.12)`,
                                border: `1px solid rgba(${hexToRgb(s.color)},0.25)`,
                                borderRadius: 14,
                                display: "flex", alignItems: "center", justifyContent: "center",
                                marginBottom: 18, color: s.color,
                            }}>
                                {s.icon}
                            </div>
                            <div style={{ fontSize: 11, fontWeight: 700, color: "var(--text-muted)", letterSpacing: "1px", marginBottom: 6 }}>
                                STEP {s.num}
                            </div>
                            <h3 className="section-title" style={{ marginBottom: 10 }}>{s.title}</h3>
                            <p className="section-subtitle" style={{ fontSize: 13 }}>{s.desc}</p>
                        </div>
                    ))}
                </div>

                {/* ML metrics */}
                <div className="glass-card" style={{
                    padding: 32, marginBottom: 40,
                    background: "linear-gradient(135deg,rgba(79,142,255,0.07),rgba(0,229,255,0.04))"
                }}>
                    <div className="section-title" style={{ marginBottom: 20, textAlign: "center" }}>Model Performance</div>
                    <div className="grid-4">
                        {[
                            { label: "Training Samples", value: "50,000", color: "var(--primary)" },
                            { label: "Features", value: "15", color: "var(--accent)" },
                            { label: "CV Accuracy", value: "≥90%", color: "var(--success)" },
                            { label: "Algorithm", value: "GB+RF", color: "var(--accent2)" },
                        ].map(({ label, value, color }) => (
                            <div key={label} style={{ textAlign: "center" }}>
                                <div style={{ fontSize: 32, fontWeight: 900, color, marginBottom: 4 }}>{value}</div>
                                <div className="label-sm">{label}</div>
                            </div>
                        ))}
                    </div>
                </div>

                {/* Tech stack */}
                <div>
                    <div className="section-title" style={{ marginBottom: 20, textAlign: "center" }}>Technology Stack</div>
                    <div className="grid-4">
                        {STACK.map(({ name, role, color }) => (
                            <div key={name} className="glass-card-sm" style={{ padding: 16 }}>
                                <div style={{ width: 8, height: 8, borderRadius: "50%", background: color, marginBottom: 10 }} />
                                <div style={{ fontSize: 13, fontWeight: 700, color: "var(--text-primary)", marginBottom: 4 }}>{name}</div>
                                <div style={{ fontSize: 11, color: "var(--text-muted)" }}>{role}</div>
                            </div>
                        ))}
                    </div>
                </div>
            </div>
        </div>
    );
}

// Helper — accepts CSS var names and hex
function hexToRgb(color) {
    if (color.startsWith("var")) return "79,142,255";
    const m = color.match(/^#([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i);
    return m ? `${parseInt(m[1], 16)},${parseInt(m[2], 16)},${parseInt(m[3], 16)}` : "99,140,255";
}
