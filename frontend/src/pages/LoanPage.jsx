import React, { useEffect, useRef, useState } from "react";
import axios from "axios";
import { AlertCircle, DollarSign } from "lucide-react";
import LoanDecisionCard from "../components/LoanDecisionCard";

const BACKEND = process.env.REACT_APP_BACKEND_URL || "http://localhost:8080";
const ETH_RE = /^0x[a-fA-F0-9]{40}$/;

const PURPOSES = [
    { value: "trading", label: "Leveraged Trading" },
    { value: "yield_farming", label: "Yield Farming" },
    { value: "leverage", label: "Portfolio Leverage" },
    { value: "nft", label: "NFT Purchase" },
    { value: "other", label: "Other" },
];

const TIERS = [
    { tier: "PLATINUM", rate: "3.5%", limit: "Unlimited", color: "var(--warning)", desc: "Exceptional credit — full DeFi history, high collateral." },
    { tier: "GOLD", rate: "6.0%", limit: "$50,000", color: "#fbbf24", desc: "Strong wallet with solid DeFi track record." },
    { tier: "SILVER", rate: "9.5%", limit: "$5,000", color: "#94a3b8", desc: "Acceptable history — small to mid-size loans." },
    { tier: "BRONZE", rate: "14%", limit: "$1,000", color: "#cd7f32", desc: "Limited history — micro loans only." },
    { tier: "REJECTED", rate: "N/A", limit: "$0", color: "var(--danger)", desc: "High risk indicators — loan unavailable." },
];

export default function LoanPage() {
    const [address, setAddress] = useState("");
    const [amount, setAmount] = useState(5000);
    const [purpose, setPurpose] = useState("trading");
    const [purposeOpen, setPurposeOpen] = useState(false);
    const [result, setResult] = useState(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");
    const purposeDropdownRef = useRef(null);

    const selectedPurpose = PURPOSES.find((item) => item.value === purpose) || PURPOSES[0];

    useEffect(() => {
        function onDocumentMouseDown(event) {
            if (!purposeDropdownRef.current?.contains(event.target)) {
                setPurposeOpen(false);
            }
        }

        function onDocumentKeyDown(event) {
            if (event.key === "Escape") {
                setPurposeOpen(false);
            }
        }

        document.addEventListener("mousedown", onDocumentMouseDown);
        document.addEventListener("keydown", onDocumentKeyDown);
        return () => {
            document.removeEventListener("mousedown", onDocumentMouseDown);
            document.removeEventListener("keydown", onDocumentKeyDown);
        };
    }, []);

    async function apply() {
        const addr = address.trim();
        if (!ETH_RE.test(addr)) { setError("Enter a valid EVM wallet address"); return; }
        setError(""); setResult(null); setLoading(true);
        try {
            const resp = await axios.post(`${BACKEND}/loan/evaluate`, {
                walletAddress: addr,
                amount,
                purpose,
            });
            setResult(resp.data);
        } catch (err) {
            setError(err?.response?.data?.message || err?.response?.data?.detail || err.message || "Request failed");
        } finally {
            setLoading(false);
        }
    }

    function handleKey(e) { if (e.key === "Enter") apply(); }

    return (
        <div className="section">
            <div className="container">
                <div style={{ marginBottom: 32 }}>
                    <span className="badge badge-purple" style={{ marginBottom: 12 }}>
                        <DollarSign size={12} /> DeFi Loans
                    </span>
                    <h1 className="page-title" style={{ fontSize: 34 }}>
                        Apply for <span className="gradient-text">DeFi Loan</span>
                    </h1>
                    <p className="section-subtitle" style={{ marginTop: 8, fontSize: 15 }}>
                        Our ML model evaluates your on-chain credit profile and delivers an instant decision with full reasoning.
                    </p>
                </div>

                <div className="grid-2" style={{ gap: 28, alignItems: "start" }}>

                    {/* Application form */}
                    <div style={{ display: "flex", flexDirection: "column", gap: 20 }}>
                        <div className="glass-card" style={{ padding: 28 }}>
                            <div className="section-title" style={{ marginBottom: 20 }}>Loan Application</div>

                            {/* Wallet */}
                            <div style={{ marginBottom: 18 }}>
                                <label className="label-sm" style={{ display: "block", marginBottom: 6 }}>Wallet Address</label>
                                <input
                                    className="ct-input"
                                    value={address}
                                    onChange={(e) => setAddress(e.target.value)}
                                    onKeyDown={handleKey}
                                    placeholder="0x… your wallet address"
                                    style={{ width: "100%" }}
                                />
                            </div>

                            {/* Amount slider */}
                            <div style={{ marginBottom: 18 }}>
                                <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 6 }}>
                                    <label className="label-sm">Loan Amount</label>
                                    <span style={{ fontSize: 16, fontWeight: 700, color: "var(--primary)" }}>
                                        ${amount.toLocaleString()}
                                    </span>
                                </div>
                                <input
                                    type="range"
                                    className="range-slider"
                                    min={100}
                                    max={100000}
                                    step={100}
                                    value={amount}
                                    onChange={(e) => setAmount(Number(e.target.value))}
                                />
                                <div style={{ display: "flex", justifyContent: "space-between", fontSize: 11, color: "var(--text-muted)", marginTop: 4 }}>
                                    <span>$100</span><span>$100,000</span>
                                </div>
                            </div>

                            {/* Purpose */}
                            <div style={{ marginBottom: 24 }}>
                                <label className="label-sm" style={{ display: "block", marginBottom: 6 }}>Loan Purpose</label>
                                <div className="ct-dropdown" ref={purposeDropdownRef}>
                                    <button
                                        type="button"
                                        className={`ct-dropdown-trigger${purposeOpen ? " open" : ""}`}
                                        onClick={() => setPurposeOpen((prev) => !prev)}
                                        aria-haspopup="listbox"
                                        aria-expanded={purposeOpen}
                                    >
                                        <span>{selectedPurpose.label}</span>
                                        <span className="ct-dropdown-chevron" aria-hidden="true">v</span>
                                    </button>
                                    {purposeOpen && (
                                        <div className="ct-dropdown-menu" role="listbox" aria-label="Loan purpose options">
                                            {PURPOSES.map((item) => (
                                                <button
                                                    type="button"
                                                    key={item.value}
                                                    className={`ct-dropdown-option${purpose === item.value ? " active" : ""}`}
                                                    onClick={() => {
                                                        setPurpose(item.value);
                                                        setPurposeOpen(false);
                                                    }}
                                                    role="option"
                                                    aria-selected={purpose === item.value}
                                                >
                                                    {item.label}
                                                </button>
                                            ))}
                                        </div>
                                    )}
                                </div>
                            </div>

                            {error && (
                                <div className="alert alert-error" style={{ marginBottom: 16 }}>
                                    <AlertCircle size={15} /> {error}
                                </div>
                            )}

                            <button className="btn btn-primary" onClick={apply} disabled={loading} style={{ width: "100%" }}>
                                {loading
                                    ? <><div className="spinner" style={{ width: 16, height: 16, borderWidth: 2 }} /> Evaluating…</>
                                    : "Submit Application"
                                }
                            </button>
                        </div>

                        {/* Summary */}
                        <div className="glass-card" style={{ padding: 20 }}>
                            <div className="section-title" style={{ marginBottom: 4 }}>What gets evaluated</div>
                            <p className="section-subtitle" style={{ marginBottom: 16, fontSize: 13 }}>These 15 on-chain signals inform the credit decision:</p>
                            {[
                                "Wallet age & transaction history",
                                "DeFi protocol engagement (Uniswap, Aave, etc.)",
                                "Collateral ratio across positions",
                                "Flash loan & liquidation history",
                                "Rugpull contract exposure",
                                "Cross-chain activity & dormancy",
                            ].map((f) => (
                                <div key={f} className="feature-item" style={{ marginBottom: 8 }}>
                                    <span style={{ color: "var(--success)" }}>✓</span>
                                    <span style={{ fontSize: 13 }}>{f}</span>
                                </div>
                            ))}
                        </div>
                    </div>

                    {/* Result + tier table */}
                    <div style={{ display: "flex", flexDirection: "column", gap: 20 }}>
                        {result && <LoanDecisionCard loanResult={result} />}

                        {/* Credit tiers reference */}
                        <div className="glass-card" style={{ padding: 20 }}>
                            <div className="section-title" style={{ marginBottom: 14 }}>Credit Tier Reference</div>
                            <div style={{ display: "flex", flexDirection: "column", gap: 10 }}>
                                {TIERS.map(({ tier, rate, limit, color, desc }) => (
                                    <div key={tier} style={{
                                        display: "flex", alignItems: "center", gap: 12,
                                        padding: "10px 14px",
                                        background: result?.creditTier === tier ? "rgba(79,142,255,0.08)" : "transparent",
                                        borderRadius: 10,
                                        border: result?.creditTier === tier ? "1px solid var(--border-glow)" : "1px solid transparent",
                                    }}>
                                        <div style={{ width: 8, height: 8, borderRadius: "50%", background: color, flexShrink: 0 }} />
                                        <div style={{ flex: 1 }}>
                                            <div style={{ fontSize: 13, fontWeight: 700, color }}>{tier}</div>
                                            <div style={{ fontSize: 11, color: "var(--text-muted)" }}>{desc}</div>
                                        </div>
                                        <div style={{ textAlign: "right", flexShrink: 0 }}>
                                            <div style={{ fontSize: 12, fontWeight: 600, color: "var(--text-primary)" }}>{rate}</div>
                                            <div style={{ fontSize: 11, color: "var(--text-muted)" }}>{limit}</div>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}
