import React, { useState } from "react";
import axios from "axios";
import { Clock, AlertCircle } from "lucide-react";
import TxHistoryTable from "../components/TxHistoryTable";

const BACKEND = process.env.REACT_APP_BACKEND_URL || "http://localhost:8080";
const ETH_RE = /^0x[a-fA-F0-9]{40}$/;

export default function HistoryPage() {
    const [address, setAddress] = useState("");
    const [txs, setTxs] = useState(null);
    const [stats, setStats] = useState(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");

    async function fetchHistory() {
        const addr = address.trim();
        if (!ETH_RE.test(addr)) { setError("Enter a valid EVM wallet address"); return; }
        setError(""); setTxs(null); setStats(null); setLoading(true);
        try {
            const resp = await axios.get(`${BACKEND}/wallet/${addr}/history`);
            const data = resp.data;
            setTxs(data);

            // Compute local stats
            const total = data.length;
            const incoming = data.filter((t) => t.to?.toLowerCase() === addr.toLowerCase()).length;
            const totalEth = data.reduce((s, t) => s + (t.valueEth || 0), 0);
            const flags = data.reduce((acc, t) => { acc[t.riskFlag] = (acc[t.riskFlag] || 0) + 1; return acc; }, {});

            setStats({ total, incoming, outgoing: total - incoming, totalEth, flags });
        } catch (err) {
            setError(err?.response?.data?.message || err?.response?.data?.detail || err.message || "Request failed");
        } finally {
            setLoading(false);
        }
    }

    function handleKey(e) { if (e.key === "Enter") fetchHistory(); }

    return (
        <div className="section">
            <div className="container">
                <div style={{ marginBottom: 32 }}>
                    <span className="badge badge-info" style={{ marginBottom: 12 }}>
                        <Clock size={12} /> Transaction History
                    </span>
                    <h1 className="page-title" style={{ fontSize: 34 }}>
                        Blockchain <span className="gradient-text">History</span>
                    </h1>
                    <p className="section-subtitle" style={{ marginTop: 8, fontSize: 15 }}>
                        Browse real transaction history with protocol detection and risk classification for any wallet.
                    </p>
                </div>

                {/* Search */}
                <div className="glass-card" style={{ padding: 24, marginBottom: 24 }}>
                    <div className="input-group">
                        <input
                            className="ct-input"
                            value={address}
                            onChange={(e) => setAddress(e.target.value)}
                            onKeyDown={handleKey}
                            placeholder="0x… wallet address"
                        />
                        <button className="btn btn-primary" onClick={fetchHistory} disabled={loading}>
                            {loading
                                ? <><div className="spinner" style={{ width: 16, height: 16, borderWidth: 2 }} /> Loading…</>
                                : "Fetch History"
                            }
                        </button>
                    </div>
                    {error && (
                        <div className="alert alert-error" style={{ marginTop: 12 }}>
                            <AlertCircle size={15} /> {error}
                        </div>
                    )}
                </div>

                {/* Stats row */}
                {stats && (
                    <div className="stat-grid" style={{ marginBottom: 24 }}>
                        <div className="stat-card">
                            <div className="stat-value blue">{stats.total}</div>
                            <div className="stat-label">Total Transactions</div>
                        </div>
                        <div className="stat-card">
                            <div className="stat-value green">{stats.incoming}</div>
                            <div className="stat-label">Incoming</div>
                        </div>
                        <div className="stat-card">
                            <div className="stat-value amber">{stats.outgoing}</div>
                            <div className="stat-label">Outgoing</div>
                        </div>
                        <div className="stat-card">
                            <div className="stat-value cyan">{stats.totalEth.toFixed(4)}</div>
                            <div className="stat-label">Total ETH Volume</div>
                        </div>
                        {stats.flags.FLASH_LOAN > 0 && (
                            <div className="stat-card">
                                <div className="stat-value" style={{ color: "var(--warning)" }}>{stats.flags.FLASH_LOAN}</div>
                                <div className="stat-label">Flash Loans</div>
                            </div>
                        )}
                        {stats.flags.LIQUIDATION > 0 && (
                            <div className="stat-card">
                                <div className="stat-value" style={{ color: "var(--danger)" }}>{stats.flags.LIQUIDATION}</div>
                                <div className="stat-label">Liquidations</div>
                            </div>
                        )}
                    </div>
                )}

                {/* Table */}
                {txs && (
                    <div className="glass-card" style={{ padding: 24 }}>
                        <div className="section-title" style={{ marginBottom: 16 }}>
                            Transactions{txs.length > 0 ? ` (${txs.length} loaded)` : ""}
                        </div>
                        <TxHistoryTable txs={txs} walletAddress={address.trim()} />
                    </div>
                )}

                {txs?.length === 0 && (
                    <div className="glass-card" style={{ padding: 48, textAlign: "center" }}>
                        <div style={{ fontSize: 40, marginBottom: 12 }}>🔍</div>
                        <div style={{ color: "var(--text-secondary)" }}>No transactions found for this address.</div>
                    </div>
                )}
            </div>
        </div>
    );
}
