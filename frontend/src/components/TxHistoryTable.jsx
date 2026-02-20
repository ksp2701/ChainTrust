import React, { useState } from "react";
import { ArrowUpRight, ArrowDownLeft, Zap, Image, AlertTriangle, CheckCircle } from "lucide-react";

const FLAG_META = {
    NORMAL: { cls: "badge-info", icon: <CheckCircle size={11} />, label: "Normal" },
    FLASH_LOAN: { cls: "badge-warning", icon: <Zap size={11} />, label: "Flash Loan" },
    LIQUIDATION: { cls: "badge-danger", icon: <AlertTriangle size={11} />, label: "Liquidation" },
    NFT: { cls: "badge-purple", icon: <Image size={11} />, label: "NFT" },
    RUGPULL: { cls: "badge-rejected", icon: <AlertTriangle size={11} />, label: "Rugpull" },
};

export default function TxHistoryTable({ txs = [], walletAddress = "" }) {
    const [filter, setFilter] = useState("ALL");
    const [sortDesc, setSortDesc] = useState(true);

    const flags = ["ALL", "NORMAL", "FLASH_LOAN", "LIQUIDATION", "NFT", "RUGPULL"];
    const filtered = txs
        .filter((t) => filter === "ALL" || t.riskFlag === filter)
        .sort((a, b) => sortDesc ? b.timestamp - a.timestamp : a.timestamp - b.timestamp);

    function formatTs(ts) {
        if (!ts) return "-";
        const d = new Date(ts * 1000);
        return d.toLocaleDateString("en-US", { month: "short", day: "numeric", year: "numeric" });
    }

    function shortAddr(addr) {
        if (!addr) return "-";
        return `${addr.slice(0, 8)}…${addr.slice(-6)}`;
    }

    function ethLink(hash) {
        return `https://etherscan.io/tx/${hash}`;
    }

    return (
        <div>
            {/* Filter chips */}
            <div style={{ display: "flex", gap: 8, marginBottom: 16, flexWrap: "wrap" }}>
                {flags.map((f) => (
                    <button
                        key={f}
                        onClick={() => setFilter(f)}
                        className={`btn btn-sm ${filter === f ? "btn-primary" : "btn-ghost"}`}
                        style={{ fontFamily: "var(--font-sans)", fontSize: 11, padding: "5px 12px" }}
                    >
                        {f.replace("_", " ")}
                        {f !== "ALL" && (
                            <span style={{ marginLeft: 4, opacity: 0.7 }}>
                                {txs.filter((t) => t.riskFlag === f).length}
                            </span>
                        )}
                    </button>
                ))}
                <button
                    onClick={() => setSortDesc(!sortDesc)}
                    className="btn btn-ghost btn-sm"
                    style={{ marginLeft: "auto" }}
                >
                    {sortDesc ? "↓ Newest" : "↑ Oldest"}
                </button>
            </div>

            {/* Table */}
            <div style={{ overflowX: "auto" }}>
                {filtered.length === 0 ? (
                    <div style={{ textAlign: "center", padding: 40, color: "var(--text-muted)" }}>
                        No transactions match this filter.
                    </div>
                ) : (
                    <table className="ct-table">
                        <thead>
                            <tr>
                                <th>Date</th>
                                <th>Tx Hash</th>
                                <th>Direction</th>
                                <th>Value (ETH)</th>
                                <th>Protocol</th>
                                <th>Risk Flag</th>
                            </tr>
                        </thead>
                        <tbody>
                            {filtered.slice(0, 50).map((tx) => {
                                const isIncoming = tx.to?.toLowerCase() === walletAddress.toLowerCase();
                                const meta = FLAG_META[tx.riskFlag] || FLAG_META.NORMAL;
                                return (
                                    <tr key={tx.hash}>
                                        <td style={{ color: "var(--text-secondary)", fontSize: 12 }}>{formatTs(tx.timestamp)}</td>
                                        <td>
                                            <a
                                                href={ethLink(tx.hash)}
                                                target="_blank"
                                                rel="noreferrer"
                                                className="mono"
                                                style={{ color: "var(--primary)", textDecoration: "none" }}
                                            >
                                                {shortAddr(tx.hash)}
                                            </a>
                                        </td>
                                        <td>
                                            <span style={{ display: "flex", alignItems: "center", gap: 5, fontSize: 12 }}>
                                                {isIncoming
                                                    ? <><ArrowDownLeft size={13} color="var(--success)" /> In</>
                                                    : <><ArrowUpRight size={13} color="var(--danger)" /> Out</>
                                                }
                                            </span>
                                        </td>
                                        <td style={{ fontFamily: "var(--font-mono)", fontSize: 12 }}>
                                            {(tx.valueEth || 0).toFixed(4)}
                                        </td>
                                        <td>
                                            {tx.protocol && (
                                                <span className="badge badge-cyan" style={{ fontSize: 10 }}>{tx.protocol}</span>
                                            )}
                                        </td>
                                        <td>
                                            <span className={`badge ${meta.cls}`} style={{ fontSize: 10 }}>
                                                {meta.icon}&nbsp;{meta.label}
                                            </span>
                                        </td>
                                    </tr>
                                );
                            })}
                        </tbody>
                    </table>
                )}
            </div>
            {filtered.length > 50 && (
                <div style={{ marginTop: 10, textAlign: "center", fontSize: 12, color: "var(--text-muted)" }}>
                    Showing first 50 of {filtered.length} transactions
                </div>
            )}
        </div>
    );
}
