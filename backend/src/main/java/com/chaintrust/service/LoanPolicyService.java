package com.chaintrust.service;

import com.chaintrust.model.LoanRequest;
import com.chaintrust.model.RiskResult;
import com.chaintrust.model.WalletFeatures;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Service
public class LoanPolicyService {

    private final ObjectMapper objectMapper;
    private final String thresholdsFilePath;
    private final boolean rejectBurnAddresses;
    private final boolean rejectKnownContractAddresses;
    private final boolean rejectContractAddresses;
    private final double rejectRugpullExposureGte;
    private final long rejectWalletAgeDaysLt;
    private final int rejectLiquidationEventsGte;
    private final long maxRecommendedLimitUsd;

    private volatile Thresholds thresholds;
    private volatile long thresholdsLoadedAtMs = 0L;

    public LoanPolicyService(
            ObjectMapper objectMapper,
            @Value("${loan.policy.thresholds-file:/app/model/policy_thresholds.json}") String thresholdsFilePath,
            @Value("${loan.policy.reject-burn-addresses:true}") boolean rejectBurnAddresses,
            @Value("${loan.policy.reject-known-contract-addresses:true}") boolean rejectKnownContractAddresses,
            @Value("${loan.policy.reject-contract-addresses:true}") boolean rejectContractAddresses,
            @Value("${loan.policy.reject-rugpull-exposure-gte:0.70}") double rejectRugpullExposureGte,
            @Value("${loan.policy.reject-wallet-age-days-lt:14}") long rejectWalletAgeDaysLt,
            @Value("${loan.policy.reject-liquidation-events-gte:3}") int rejectLiquidationEventsGte,
            @Value("${loan.policy.max-recommended-limit-usd:100000}") long maxRecommendedLimitUsd,
            @Value("${loan.policy.default-thresholds.platinum-min-trust:0.85}") double defaultPlatinumMinTrust,
            @Value("${loan.policy.default-thresholds.gold-min-trust:0.70}") double defaultGoldMinTrust,
            @Value("${loan.policy.default-thresholds.silver-min-trust:0.55}") double defaultSilverMinTrust,
            @Value("${loan.policy.default-thresholds.bronze-min-trust:0.40}") double defaultBronzeMinTrust,
            @Value("${loan.policy.default-thresholds.silver-max-amount:5000}") double defaultSilverMaxAmount,
            @Value("${loan.policy.default-thresholds.bronze-max-amount:1000}") double defaultBronzeMaxAmount) {
        this.objectMapper = objectMapper;
        this.thresholdsFilePath = thresholdsFilePath;
        this.rejectBurnAddresses = rejectBurnAddresses;
        this.rejectKnownContractAddresses = rejectKnownContractAddresses;
        this.rejectContractAddresses = rejectContractAddresses;
        this.rejectRugpullExposureGte = rejectRugpullExposureGte;
        this.rejectWalletAgeDaysLt = rejectWalletAgeDaysLt;
        this.rejectLiquidationEventsGte = rejectLiquidationEventsGte;
        this.maxRecommendedLimitUsd = maxRecommendedLimitUsd;
        this.thresholds = new Thresholds(
                defaultPlatinumMinTrust,
                defaultGoldMinTrust,
                defaultSilverMinTrust,
                defaultBronzeMinTrust,
                defaultSilverMaxAmount,
                defaultBronzeMaxAmount
        );
    }

    public PolicyDecision evaluate(
            LoanRequest request,
            WalletFeatures features,
            RiskResult riskResult,
            AddressIntelligenceService.AddressAssessment addressAssessment) {
        maybeReloadThresholds();
        Thresholds t = this.thresholds;

        double riskScore = clamp(riskResult.getRiskScore(), 0.0, 1.0);
        double trustScore = 1.0 - riskScore;

        String creditTier;
        boolean approved;
        double maxLoanMultiplier;
        double interestRatePercent;

        if (trustScore >= t.platinumMinTrust) {
            creditTier = "PLATINUM";
            approved = true;
            maxLoanMultiplier = 5.0;
            interestRatePercent = 3.5;
        } else if (trustScore >= t.goldMinTrust) {
            creditTier = "GOLD";
            approved = true;
            maxLoanMultiplier = 3.0;
            interestRatePercent = 6.0;
        } else if (trustScore >= t.silverMinTrust) {
            creditTier = "SILVER";
            approved = request.getAmount() <= t.silverMaxAmount;
            maxLoanMultiplier = 1.5;
            interestRatePercent = 9.5;
        } else if (trustScore >= t.bronzeMinTrust) {
            creditTier = "BRONZE";
            approved = request.getAmount() <= t.bronzeMaxAmount;
            maxLoanMultiplier = 0.75;
            interestRatePercent = 14.0;
        } else {
            creditTier = "REJECTED";
            approved = false;
            maxLoanMultiplier = 0;
            interestRatePercent = 0;
        }

        double recommendedLimit = features.getTotalVolumeEth() > 0
                ? Math.min(features.getTotalVolumeEth() * maxLoanMultiplier * 2000, maxRecommendedLimitUsd)
                : maxLoanMultiplier * 1000;

        List<String> hardRules = new ArrayList<>();
        if (rejectBurnAddresses && addressAssessment.burnAddress()) {
            hardRules.add("Hard reject: burn/null address");
        }
        if (rejectKnownContractAddresses && addressAssessment.knownProtocolContract()) {
            hardRules.add("Hard reject: known protocol/router contract address");
        }
        if (rejectContractAddresses && addressAssessment.smartContract()) {
            hardRules.add("Hard reject: smart contract address; only EOA wallets are eligible");
        }
        if (features.getRugpullExposureScore() >= rejectRugpullExposureGte) {
            hardRules.add("Hard reject: rugpull exposure exceeds policy threshold");
        }
        if (features.getWalletAgeDays() < rejectWalletAgeDaysLt) {
            hardRules.add("Hard reject: wallet age below minimum policy threshold");
        }
        if (features.getLiquidationEvents() >= rejectLiquidationEventsGte) {
            hardRules.add("Hard reject: excessive liquidation history");
        }

        if (!hardRules.isEmpty()) {
            return new PolicyDecision(
                    false,
                    "REJECTED",
                    trustScore,
                    riskScore,
                    0,
                    0,
                    hardRules,
                    true
            );
        }

        if (approved && request.getAmount() > recommendedLimit) {
            approved = false;
            if ("PLATINUM".equals(creditTier) || "GOLD".equals(creditTier)) {
                creditTier = "SILVER";
            }
            hardRules.add("Policy reject: requested amount exceeds recommended limit");
        }

        if (!approved) {
            interestRatePercent = 0;
        }

        return new PolicyDecision(
                approved,
                creditTier,
                trustScore,
                riskScore,
                interestRatePercent,
                Math.round(Math.max(0.0, recommendedLimit)),
                hardRules,
                false
        );
    }

    private void maybeReloadThresholds() {
        long now = System.currentTimeMillis();
        if (now - thresholdsLoadedAtMs < 60_000) {
            return;
        }

        File f = new File(thresholdsFilePath);
        if (!f.exists() || !f.isFile()) {
            thresholdsLoadedAtMs = now;
            return;
        }

        try {
            JsonNode node = objectMapper.readTree(f);
            JsonNode root = node.has("thresholds") ? node.get("thresholds") : node;
            Thresholds loaded = new Thresholds(
                    readDouble(root, "platinum_min_trust", thresholds.platinumMinTrust),
                    readDouble(root, "gold_min_trust", thresholds.goldMinTrust),
                    readDouble(root, "silver_min_trust", thresholds.silverMinTrust),
                    readDouble(root, "bronze_min_trust", thresholds.bronzeMinTrust),
                    readDouble(root, "silver_max_amount", thresholds.silverMaxAmount),
                    readDouble(root, "bronze_max_amount", thresholds.bronzeMaxAmount)
            );

            if (loaded.platinumMinTrust >= loaded.goldMinTrust
                    && loaded.goldMinTrust >= loaded.silverMinTrust
                    && loaded.silverMinTrust >= loaded.bronzeMinTrust) {
                this.thresholds = loaded;
            }
        } catch (Exception ignored) {
        } finally {
            thresholdsLoadedAtMs = now;
        }
    }

    private static double readDouble(JsonNode node, String key, double fallback) {
        if (node == null || !node.has(key) || node.get(key).isNull()) {
            return fallback;
        }
        return node.get(key).asDouble(fallback);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private record Thresholds(
            double platinumMinTrust,
            double goldMinTrust,
            double silverMinTrust,
            double bronzeMinTrust,
            double silverMaxAmount,
            double bronzeMaxAmount
    ) {}

    public record PolicyDecision(
            boolean approved,
            String creditTier,
            double trustScore,
            double riskScore,
            double interestRatePercent,
            long recommendedLimit,
            List<String> policyReasons,
            boolean hardRejected
    ) {}
}
