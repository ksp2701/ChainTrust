import React, { useEffect, useRef, useState } from "react";

const RISK_COLORS = {
    LOW: { stroke: "#10b981", glow: "rgba(16,185,129,0.4)", text: "#34d399" },
    MEDIUM: { stroke: "#f59e0b", glow: "rgba(245,158,11,0.4)", text: "#fbbf24" },
    HIGH: { stroke: "#ef4444", glow: "rgba(239,68,68,0.4)", text: "#f87171" },
};

export default function RiskGauge({ riskScore = 0, riskLevel = "LOW" }) {
    const [animated, setAnimated] = useState(0);
    const raf = useRef(null);

    const colors = RISK_COLORS[riskLevel] || RISK_COLORS.LOW;
    const cx = 90, cy = 90, r = 70;
    const startAngle = 230 * (Math.PI / 180);
    const totalAngle = 260 * (Math.PI / 180); // 260° sweep
    const circumference = r * totalAngle;

    // Animate from 0 to riskScore
    useEffect(() => {
        const start = Date.now();
        const duration = 1200;
        const from = 0;
        const to = riskScore;

        function frame() {
            const elapsed = Date.now() - start;
            const progress = Math.min(elapsed / duration, 1);
            const eased = 1 - Math.pow(1 - progress, 3); // ease-out-cubic
            setAnimated(from + (to - from) * eased);
            if (progress < 1) raf.current = requestAnimationFrame(frame);
        }
        raf.current = requestAnimationFrame(frame);
        return () => cancelAnimationFrame(raf.current);
    }, [riskScore]);

    const arcX = (angle) => cx + r * Math.cos(angle);
    const arcY = (angle) => cy + r * Math.sin(angle);

    const pathArc = (sweepFraction) => {
        const endAngle = startAngle - sweepFraction * totalAngle; // counter-clockwise mapping
        const largeArc = sweepFraction > 0.5 ? 1 : 0;
        return `M ${arcX(startAngle)} ${arcY(startAngle)} A ${r} ${r} 0 ${largeArc} 0 ${arcX(endAngle)} ${arcY(endAngle)}`;
    };

    const trustPct = Math.round((1 - animated) * 100);
    const riskPct = Math.round(animated * 100);

    return (
        <div className="risk-gauge-wrap">
            <svg width={180} height={180} className="gauge-svg" viewBox="0 0 180 180">
                <defs>
                    <filter id="glow">
                        <feGaussianBlur stdDeviation="3" result="blur" />
                        <feComposite in="SourceGraphic" in2="blur" operator="over" />
                    </filter>
                </defs>

                {/* Background arc */}
                <path d={pathArc(1)} fill="none" stroke="rgba(255,255,255,0.06)" strokeWidth={12} strokeLinecap="round" />

                {/* Coloured progress arc */}
                <path
                    d={pathArc(animated)}
                    fill="none"
                    stroke={colors.stroke}
                    strokeWidth={12}
                    strokeLinecap="round"
                    filter="url(#glow)"
                    style={{ transition: "none" }}
                />

                {/* Center text */}
                <text x={cx} y={cy - 8} textAnchor="middle" fill={colors.text} fontSize={30} fontWeight={800}>
                    {riskPct}%
                </text>
                <text x={cx} y={cy + 14} textAnchor="middle" fill="rgba(255,255,255,0.4)" fontSize={11}>
                    RISK SCORE
                </text>
                <text x={cx} y={cy + 34} textAnchor="middle" fill={colors.text} fontSize={13} fontWeight={700}>
                    {riskLevel}
                </text>
            </svg>

            <div style={{ display: "flex", gap: 20, marginTop: 4 }}>
                <div style={{ textAlign: "center" }}>
                    <div style={{ fontSize: 20, fontWeight: 800, color: "var(--success)" }}>{trustPct}%</div>
                    <div style={{ fontSize: 11, color: "var(--text-muted)", fontWeight: 500, textTransform: "uppercase", letterSpacing: "0.5px" }}>Trust</div>
                </div>
                <div style={{ width: 1, background: "var(--border)" }} />
                <div style={{ textAlign: "center" }}>
                    <div style={{ fontSize: 20, fontWeight: 800, color: colors.text }}>{riskPct}%</div>
                    <div style={{ fontSize: 11, color: "var(--text-muted)", fontWeight: 500, textTransform: "uppercase", letterSpacing: "0.5px" }}>Risk</div>
                </div>
            </div>
        </div>
    );
}
