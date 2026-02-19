package com.chaintrust.model;

import com.fasterxml.jackson.annotation.JsonAlias;

public class RiskResult {

    @JsonAlias("risk_score")
    private double riskScore;

    @JsonAlias("risk_level")
    private String riskLevel;

    private String reason;

    public RiskResult() {
    }

    public RiskResult(double riskScore, String riskLevel, String reason) {
        this.riskScore = clampScore(riskScore);
        this.riskLevel = normalizeLevel(riskLevel, this.riskScore);
        this.reason = reason;
    }

    public static RiskResult highRisk(String reason) {
        return new RiskResult(1.0, "HIGH", reason);
    }

    public RiskResult normalized() {
        this.riskScore = clampScore(this.riskScore);
        this.riskLevel = normalizeLevel(this.riskLevel, this.riskScore);
        return this;
    }

    public double getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(double riskScore) {
        this.riskScore = clampScore(riskScore);
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = normalizeLevel(riskLevel, this.riskScore);
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    private static double clampScore(double score) {
        if (score < 0.0) {
            return 0.0;
        }
        if (score > 1.0) {
            return 1.0;
        }
        return score;
    }

    private static String normalizeLevel(String level, double score) {
        if (level != null && !level.isBlank()) {
            return level.toUpperCase();
        }
        if (score < 0.4) {
            return "LOW";
        }
        if (score < 0.7) {
            return "MEDIUM";
        }
        return "HIGH";
    }
}
