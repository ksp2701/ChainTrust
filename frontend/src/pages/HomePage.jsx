import React, { useEffect, useRef, useState } from "react";
import { Link } from "react-router-dom";
import { Shield, TrendingUp, Zap, ChevronRight, Lock, Globe, Activity } from "lucide-react";

function AnimatedCounter({ target, suffix = "", duration = 2000 }) {
    const [value, setValue] = useState(0);
    const raf = useRef(null);

    useEffect(() => {
        const start = Date.now();
        function step() {
            const p = Math.min((Date.now() - start) / duration, 1);
            const eased = 1 - Math.pow(1 - p, 3);
            setValue(Math.round(target * eased));
            if (p < 1) raf.current = requestAnimationFrame(step);
        }
        raf.current = requestAnimationFrame(step);
        return () => cancelAnimationFrame(raf.current);
    }, [target, duration]);

    return <span>{value.toLocaleString()}{suffix}</span>;
}

const FEATURES = [
    { icon: <Shield size={22} />, title: "15 DeFi Features", desc: "Wallet age, DeFi protocol usage, collateral ratio, flash loans, liquidation history and more feed our ML model." },
    { icon: <Activity size={22} />, title: "90%+ ML Accuracy", desc: "Gradient Boosting ensemble trained on 50,000 samples with isotonic probability calibration." },
    { icon: <Globe size={22} />, title: "Real Blockchain Data", desc: "Actual Etherscan transaction history — not fake data. Every feature is derived from your on-chain footprint." },
    { icon: <Lock size={22} />, title: "On-Chain Proof", desc: "Every loan decision is SHA-256 hashed and can be anchored on-chain via the ChainTrust smart contract." },
    { icon: <Zap size={22} />, title: "5-Tier Credit System", desc: "PLATINUM → BRONZE tiers with appropriate interest rates (3.5%–14%) and loan limits." },
    { icon: <TrendingUp size={22} />, title: "Transparent Reasons", desc: "No black box — every approval or denial comes with a detailed breakdown of contributing factors." },
];

const STATS = [
    { value: 50000, suffix: "", label: "Training Samples" },
    { value: 15, suffix: "", label: "ML Features" },
    { value: 90, suffix: "%+", label: "Model Accuracy" },
    { value: 5, suffix: "", label: "Credit Tiers" },
];

export default function HomePage({ onOpenLogin, currentUser }) {
    return (
        <div>
            {/* Hero */}
            <section className="section" style={{ minHeight: "85vh", display: "flex", alignItems: "center" }}>
                <div className="container">
                    <div style={{ maxWidth: 720 }}>
                        <div className="badge badge-cyan fade-in-up" style={{ marginBottom: 20 }}>
                            <Zap size={12} />  DeFi Credit Protocol
                        </div>
                        <h1 className="page-title fade-in-up-1" style={{ marginBottom: 20 }}>
                            AI-Powered<br />
                            <span className="gradient-text">DeFi Loan Risk</span><br />
                            Intelligence
                        </h1>
                        <p style={{ fontSize: 17, color: "var(--text-secondary)", lineHeight: 1.7, marginBottom: 32, maxWidth: 560 }} className="fade-in-up-2">
                            Analyze any EVM wallet address. Get a real-time ML risk score derived from
                            actual blockchain history, DeFi interactions, and credit signals — with
                            transparent reasons for every decision.
                        </p>
                        <div style={{ display: "flex", gap: 12, flexWrap: "wrap" }} className="fade-in-up-3">
                            <Link to="/analyze" className="btn btn-primary btn-lg">
                                Analyze Wallet <ChevronRight size={18} />
                            </Link>
                            <Link to="/loan" className="btn btn-ghost btn-lg">
                                Apply for Loan
                            </Link>
                            {!currentUser && (
                                <button type="button" className="btn btn-ghost btn-lg" onClick={onOpenLogin}>
                                    Login / Register
                                </button>
                            )}
                        </div>
                    </div>
                </div>
            </section>

            {/* Stats */}
            <section style={{ borderTop: "1px solid var(--border)", borderBottom: "1px solid var(--border)", padding: "32px 0", background: "rgba(12,22,48,0.4)" }}>
                <div className="container">
                    <div className="grid-4">
                        {STATS.map(({ value, suffix, label }) => (
                            <div key={label} className="stat-card" style={{ textAlign: "center" }}>
                                <div className="stat-value blue" style={{ fontSize: 36 }}>
                                    <AnimatedCounter target={value} suffix={suffix} />
                                </div>
                                <div className="stat-label">{label}</div>
                            </div>
                        ))}
                    </div>
                </div>
            </section>

            {/* Features */}
            <section className="section">
                <div className="container">
                    <div style={{ textAlign: "center", marginBottom: 48 }}>
                        <h2 className="page-title" style={{ fontSize: 32 }}>
                            Why <span className="gradient-text">ChainTrust</span>?
                        </h2>
                        <p style={{ color: "var(--text-secondary)", marginTop: 8, fontSize: 15 }}>
                            Built on real on-chain data with explainable AI.
                        </p>
                    </div>
                    <div className="grid-3">
                        {FEATURES.map((f, i) => (
                            <div key={i} className="glass-card" style={{ padding: 24 }}>
                                <div style={{
                                    width: 44, height: 44,
                                    background: "linear-gradient(135deg,rgba(79,142,255,0.2),rgba(0,229,255,0.1))",
                                    borderRadius: 12,
                                    display: "flex", alignItems: "center", justifyContent: "center",
                                    marginBottom: 16, color: "var(--primary)"
                                }}>
                                    {f.icon}
                                </div>
                                <h3 className="section-title" style={{ marginBottom: 8 }}>{f.title}</h3>
                                <p className="section-subtitle">{f.desc}</p>
                            </div>
                        ))}
                    </div>
                </div>
            </section>

            {/* CTA */}
            <section className="section">
                <div className="container">
                    <div className="glass-card" style={{
                        padding: 48, textAlign: "center",
                        background: "linear-gradient(135deg,rgba(79,142,255,0.08),rgba(0,229,255,0.05))"
                    }}>
                        <h2 className="page-title" style={{ fontSize: 30, marginBottom: 12 }}>
                            Ready to check your <span className="gradient-text">DeFi credit</span>?
                        </h2>
                        <p style={{ color: "var(--text-secondary)", marginBottom: 32, fontSize: 15 }}>
                            Enter any EVM address — results in seconds.
                        </p>
                        <Link to="/analyze" className="btn btn-accent btn-lg">
                            Get Started Free →
                        </Link>
                    </div>
                </div>
            </section>
        </div>
    );
}
