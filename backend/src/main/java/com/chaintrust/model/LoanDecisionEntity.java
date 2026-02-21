package com.chaintrust.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(
        name = "loan_decisions",
        indexes = {
                @Index(name = "idx_loan_decision_hash", columnList = "decisionHash", unique = true),
                @Index(name = "idx_loan_outcome_label", columnList = "outcomeLabel"),
                @Index(name = "idx_loan_created_at", columnList = "createdAt")
        }
)
public class LoanDecisionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64, unique = true)
    private String decisionHash;

    @Column(nullable = false, length = 42)
    private String walletAddress;

    @Column(nullable = false)
    private double requestedAmount;

    @Column(nullable = false)
    private double trustScore;

    @Column(nullable = false)
    private double riskScore;

    @Column(nullable = false, length = 16)
    private String riskLevel;

    @Column(nullable = false)
    private boolean approved;

    @Column(nullable = false, length = 16)
    private String creditTier;

    @Column(nullable = false)
    private double interestRatePercent;

    @Column(nullable = false)
    private long recommendedLimit;

    @Column(nullable = false, length = 32)
    private String onChainStatus;

    @Column(length = 80)
    private String onChainTxHash;

    @Column(length = 1000)
    private String onChainError;

    @Column(columnDefinition = "TEXT")
    private String featuresJson;

    @Column(columnDefinition = "TEXT")
    private String reasonsJson;

    @Column(nullable = false, length = 16)
    private String outcomeLabel = "UNKNOWN";

    @Column(nullable = false)
    private Instant createdAt;

    private Instant outcomeUpdatedAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (outcomeLabel == null || outcomeLabel.isBlank()) {
            outcomeLabel = "UNKNOWN";
        }
    }

    public Long getId() {
        return id;
    }

    public String getDecisionHash() {
        return decisionHash;
    }

    public void setDecisionHash(String decisionHash) {
        this.decisionHash = decisionHash;
    }

    public String getWalletAddress() {
        return walletAddress;
    }

    public void setWalletAddress(String walletAddress) {
        this.walletAddress = walletAddress;
    }

    public double getRequestedAmount() {
        return requestedAmount;
    }

    public void setRequestedAmount(double requestedAmount) {
        this.requestedAmount = requestedAmount;
    }

    public double getTrustScore() {
        return trustScore;
    }

    public void setTrustScore(double trustScore) {
        this.trustScore = trustScore;
    }

    public double getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(double riskScore) {
        this.riskScore = riskScore;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public boolean isApproved() {
        return approved;
    }

    public void setApproved(boolean approved) {
        this.approved = approved;
    }

    public String getCreditTier() {
        return creditTier;
    }

    public void setCreditTier(String creditTier) {
        this.creditTier = creditTier;
    }

    public double getInterestRatePercent() {
        return interestRatePercent;
    }

    public void setInterestRatePercent(double interestRatePercent) {
        this.interestRatePercent = interestRatePercent;
    }

    public long getRecommendedLimit() {
        return recommendedLimit;
    }

    public void setRecommendedLimit(long recommendedLimit) {
        this.recommendedLimit = recommendedLimit;
    }

    public String getOnChainStatus() {
        return onChainStatus;
    }

    public void setOnChainStatus(String onChainStatus) {
        this.onChainStatus = onChainStatus;
    }

    public String getOnChainTxHash() {
        return onChainTxHash;
    }

    public void setOnChainTxHash(String onChainTxHash) {
        this.onChainTxHash = onChainTxHash;
    }

    public String getOnChainError() {
        return onChainError;
    }

    public void setOnChainError(String onChainError) {
        this.onChainError = onChainError;
    }

    public String getFeaturesJson() {
        return featuresJson;
    }

    public void setFeaturesJson(String featuresJson) {
        this.featuresJson = featuresJson;
    }

    public String getReasonsJson() {
        return reasonsJson;
    }

    public void setReasonsJson(String reasonsJson) {
        this.reasonsJson = reasonsJson;
    }

    public String getOutcomeLabel() {
        return outcomeLabel;
    }

    public void setOutcomeLabel(String outcomeLabel) {
        this.outcomeLabel = outcomeLabel;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getOutcomeUpdatedAt() {
        return outcomeUpdatedAt;
    }

    public void setOutcomeUpdatedAt(Instant outcomeUpdatedAt) {
        this.outcomeUpdatedAt = outcomeUpdatedAt;
    }
}
