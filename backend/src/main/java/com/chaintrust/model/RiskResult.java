package com.chaintrust.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.util.List;
import java.util.Map;

public class RiskResult {

    @JsonAlias("risk_score")
    private double riskScore;

    @JsonAlias("risk_level")
    private String riskLevel;

    private String reason;

    @JsonAlias("feature_contributions")
    private Map<String, Double> featureContributions;

    @JsonAlias("denial_reasons")
    private List<String> denialReasons;

    public RiskResult() {}

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

    // ── Getters & Setters ──────────────────────────────────────────────────

    public double getRiskScore() { return riskScore; }
    public void setRiskScore(double riskScore) { this.riskScore = clampScore(riskScore); }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = normalizeLevel(riskLevel, this.riskScore); }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public Map<String, Double> getFeatureContributions() { return featureContributions; }
    public void setFeatureContributions(Map<String, Double> featureContributions) {
        this.featureContributions = featureContributions;
    }

    public List<String> getDenialReasons() { return denialReasons; }
    public void setDenialReasons(List<String> denialReasons) { this.denialReasons = denialReasons; }

    // ── Helpers ────────────────────────────────────────────────────────────

    private static double clampScore(double score) {
        return Math.max(0.0, Math.min(1.0, score));
    }

    private static String normalizeLevel(String level, double score) {
        if (level != null && !level.isBlank()) {
            return level.toUpperCase();
        }
        if (score < 0.35) return "LOW";
        if (score < 0.65) return "MEDIUM";
        return "HIGH";
    }
}
