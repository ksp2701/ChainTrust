import React, { useState } from "react";
import { Copy, CheckCircle, ExternalLink } from "lucide-react";

export default function BlockchainBadge({ hash }) {
    const [copied, setCopied] = useState(false);

    if (!hash) return null;

    async function copyHash() {
        await navigator.clipboard.writeText(hash);
        setCopied(true);
        setTimeout(() => setCopied(false), 2000);
    }

    const short = `${hash.slice(0, 12)}…${hash.slice(-8)}`;

    return (
        <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
            <div
                className="hash-display"
                onClick={copyHash}
                title="Click to copy"
                style={{ flexGrow: 1 }}
            >
                {hash}
            </div>
            <button
                onClick={copyHash}
                className="btn btn-ghost btn-sm"
                title={copied ? "Copied!" : "Copy hash"}
                style={{ flexShrink: 0 }}
            >
                {copied
                    ? <CheckCircle size={14} color="var(--success)" />
                    : <Copy size={14} />
                }
            </button>
        </div>
    );
}
