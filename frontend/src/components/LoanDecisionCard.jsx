import React from "react";
import { CheckCircle, XCircle, AlertTriangle, TrendingUp, Shield, Zap } from "lucide-react";
import BlockchainBadge from "./BlockchainBadge";

const TIER_META = {
    PLATINUM: { cls: "badge-platinum", icon: "💎", label: "Platinum" },
    GOLD: { cls: "badge-gold", icon: "🥇", label: "Gold" },
    SILVER: { cls: "badge-silver", icon: "🥈", label: "Silver" },
    BRONZE: { cls: "badge-bronze", icon: "🥉", label: "Bronze" },
    REJECTED: { cls: "badge-rejected", icon: "🚫", label: "Rejected" },
};

export default function LoanDecisionCard({ loanResult }) {
    if (!loanResult) return null;

    const {
        approved, creditTier = "REJECTED", trustScore, riskScore,
        interestRatePercent, recommendedLimit, reasons = [],
        decisionHash, walletAddress, amount, timestamp,
        defiProtocolCount, collateralRatio, liquidationEvents, knownProtocols = []
    } = loanResult;

    const tierMeta = TIER_META[creditTier] || TIER_META.REJECTED;
    const trustPct = Math.round((trustScore ?? 0) * 100);
    const riskPct = Math.round((riskScore ?? 0) * 100);

    return (
        <div className="glass-card" style={{ padding: 28 }}>
            {/* Header */}
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", marginBottom: 24 }}>
                <div>
                    <div style={{ display: "flex", alignItems: "center", gap: 12, marginBottom: 8 }}>
                        {approved
                            ? <CheckCircle size={28} color="var(--success)" />
                            : <XCircle size={28} color="var(--danger)" />
                        }
                        <h2 style={{ fontSize: 22, fontWeight: 800, color: approved ? "var(--success)" : "var(--danger)" }}>
                            Loan {approved ? "APPROVED" : "DENIED"}
                        </h2>
                    </div>
                    <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
                        <span className={`badge ${tierMeta.cls}`}>{tierMeta.icon} {tierMeta.label}</span>
                        <span className={`badge ${approved ? "badge-success" : "badge-danger"}`}>
                            {approved ? "✓ Eligible" : "✗ Ineligible"}
                        </span>
                        <span className="badge badge-info">{riskPct}% Risk</span>
                    </div>
                </div>
                <div style={{ textAlign: "right" }}>
                    <div style={{ fontSize: 32, fontWeight: 900, color: "var(--text-primary)" }}>
                        ${Number(amount || 0).toLocaleString()}
                    </div>
                    <div className="label-sm">Requested</div>
                </div>
            </div>

            {/* Key metrics */}
            <div className="stat-grid" style={{ marginBottom: 24 }}>
                <div className="stat-card">
                    <div className="stat-value green">{trustPct}%</div>
                    <div className="stat-label">Trust Score</div>
                </div>
                {approved && (
                    <div className="stat-card">
                        <div className="stat-value blue">{interestRatePercent}%</div>
                        <div className="stat-label">Interest Rate / yr</div>
                    </div>
                )}
                {approved && (
                    <div className="stat-card">
                        <div className="stat-value cyan">${Number(recommendedLimit || 0).toLocaleString()}</div>
                        <div className="stat-label">Recommended Limit</div>
                    </div>
                )}
                {collateralRatio != null && (
                    <div className="stat-card">
                        <div className="stat-value amber">{Number(collateralRatio).toFixed(2)}x</div>
                        <div className="stat-label">Collateral Ratio</div>
                    </div>
                )}
            </div>

            {/* DeFi profile */}
            {(defiProtocolCount > 0 || knownProtocols.length > 0) && (
                <div style={{ marginBottom: 20 }}>
                    <div className="label-sm" style={{ marginBottom: 8 }}>DeFi Activity</div>
                    <div style={{ display: "flex", flexWrap: "wrap", gap: 6 }}>
                        {knownProtocols.slice(0, 8).map((p) => (
                            <span key={p} className="badge badge-cyan" style={{ fontSize: 10 }}>{p}</span>
                        ))}
                        {defiProtocolCount > 0 && (
                            <span className="badge badge-info">{defiProtocolCount} protocols</span>
                        )}
                    </div>
                </div>
            )}

            {/* Reasons list */}
            {reasons.length > 0 && (
                <div style={{ marginBottom: 20 }}>
                    <div className="label-sm" style={{ marginBottom: 10 }}>Decision Rationale</div>
                    <ul className="feature-list">
                        {reasons.map((r, i) => (
                            <li key={i} className="feature-item" style={{ fontSize: 13, color: "var(--text-primary)" }}>
                                <span>{r}</span>
                            </li>
                        ))}
                    </ul>
                </div>
            )}

            {/* Progress bars */}
            <div style={{ display: "flex", flexDirection: "column", gap: 10, marginBottom: 20 }}>
                <div>
                    <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 4 }}>
                        <span className="label-sm">Trust</span><span style={{ fontSize: 12, color: "var(--success)" }}>{trustPct}%</span>
                    </div>
                    <div className="progress-bar-wrap">
                        <div className="progress-bar-fill" style={{ width: `${trustPct}%`, background: "linear-gradient(90deg,#10b981,#34d399)" }} />
                    </div>
                </div>
                <div>
                    <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 4 }}>
                        <span className="label-sm">Risk</span><span style={{ fontSize: 12, color: "var(--danger)" }}>{riskPct}%</span>
                    </div>
                    <div className="progress-bar-wrap">
                        <div className="progress-bar-fill" style={{ width: `${riskPct}%`, background: "linear-gradient(90deg,#ef4444,#f87171)" }} />
                    </div>
                </div>
            </div>

            {/* Blockchain hash */}
            {decisionHash && (
                <div>
                    <div className="label-sm" style={{ marginBottom: 6 }}>On-Chain Decision Hash (SHA-256)</div>
                    <BlockchainBadge hash={decisionHash} />
                    <div style={{ marginTop: 6, fontSize: 11, color: "var(--text-muted)" }}>
                        Immutable proof of this credit decision — can be stored on-chain via ChainTrust contract
                    </div>
                </div>
            )}
        </div>
    );
}
