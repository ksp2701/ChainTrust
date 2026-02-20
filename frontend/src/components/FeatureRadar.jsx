import React from "react";
import {
    RadarChart, Radar, PolarGrid, PolarAngleAxis, ResponsiveContainer, Tooltip
} from "recharts";

const FEATURE_META = {
    wallet_age_days: { label: "Age", color: "#4f8eff" },
    tx_count: { label: "Tx Count", color: "#4f8eff" },
    avg_tx_value_eth: { label: "Avg Value", color: "#f59e0b" },
    unique_contracts: { label: "Contracts", color: "#4f8eff" },
    incoming_outgoing_ratio: { label: "I/O Ratio", color: "#10b981" },
    tx_variance: { label: "Variance", color: "#f59e0b" },
    defi_protocol_count: { label: "DeFi Proto.", color: "#10b981" },
    flash_loan_count: { label: "Flash Loans", color: "#ef4444" },
    liquidation_events: { label: "Liquidations", color: "#ef4444" },
    nft_transaction_count: { label: "NFT Txns", color: "#a855f7" },
    max_single_tx_eth: { label: "Max Tx", color: "#f59e0b" },
    dormant_period_days: { label: "Dormancy", color: "#f59e0b" },
    collateral_ratio: { label: "Collateral", color: "#10b981" },
    cross_chain_count: { label: "Cross-Chain", color: "#00e5ff" },
    rugpull_exposure_score: { label: "Rugpull Exp.", color: "#ef4444" },
};

// Normalise each feature to 0-100 for the radar
const NORMS = {
    wallet_age_days: { max: 2000 },
    tx_count: { max: 500 },
    avg_tx_value_eth: { max: 10, invert: true },
    unique_contracts: { max: 100 },
    incoming_outgoing_ratio: { max: 1 },
    tx_variance: { max: 5, invert: true },
    defi_protocol_count: { max: 20 },
    flash_loan_count: { max: 10, invert: true },
    liquidation_events: { max: 5, invert: true },
    nft_transaction_count: { max: 100 },
    max_single_tx_eth: { max: 20, invert: true },
    dormant_period_days: { max: 365, invert: true },
    collateral_ratio: { max: 4 },
    cross_chain_count: { max: 20 },
    rugpull_exposure_score: { max: 1, invert: true },
};

function normalise(key, value) {
    const cfg = NORMS[key] || { max: 100 };
    const clamped = Math.min(value || 0, cfg.max);
    const pct = (clamped / cfg.max) * 100;
    return cfg.invert ? 100 - pct : pct;
}

export default function FeatureRadar({ features }) {
    if (!features) return null;

    const data = Object.keys(FEATURE_META).map((key) => ({
        subject: FEATURE_META[key].label,
        value: Math.round(normalise(key, features[key] ?? features[camelKey(key)] ?? 0)),
        fullMark: 100,
    }));

    return (
        <ResponsiveContainer width="100%" height={300}>
            <RadarChart cx="50%" cy="50%" outerRadius="75%" data={data}>
                <PolarGrid stroke="rgba(99,140,255,0.15)" />
                <PolarAngleAxis
                    dataKey="subject"
                    tick={{ fill: "rgba(176,196,240,0.7)", fontSize: 10, fontFamily: "Inter" }}
                />
                <Tooltip
                    contentStyle={{
                        background: "rgba(12,22,48,0.95)",
                        border: "1px solid rgba(99,140,255,0.2)",
                        borderRadius: 10,
                        color: "#e8f0ff",
                        fontSize: 12,
                    }}
                    formatter={(v) => [`${v}/100`, "Score"]}
                />
                <Radar
                    name="Wallet Profile"
                    dataKey="value"
                    stroke="#4f8eff"
                    fill="#4f8eff"
                    fillOpacity={0.18}
                    strokeWidth={2}
                />
            </RadarChart>
        </ResponsiveContainer>
    );
}

// Convert snake_case to camelCase for WalletFeatures Java field names
function camelKey(snake) {
    return snake.replace(/_([a-z])/g, (_, c) => c.toUpperCase());
}
