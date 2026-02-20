package com.chaintrust.service;

import com.chaintrust.model.LoanRequest;
import com.chaintrust.model.RiskResult;
import com.chaintrust.model.WalletFeatures;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@Service
public class LoanService {

    private final FeatureService featureService;
    private final WalletService walletService;
    private final RiskServiceClient riskServiceClient;

    public LoanService(FeatureService featureService, WalletService walletService, RiskServiceClient riskServiceClient) {
        this.featureService = featureService;
        this.walletService = walletService;
        this.riskServiceClient = riskServiceClient;
    }

    public Map<String, Object> evaluate(LoanRequest loanRequest) {
        featureService.requireValidAddress(loanRequest.getWalletAddress());

        WalletFeatures features = walletService.extractFeatures(loanRequest.getWalletAddress());
        RiskResult riskResult = riskServiceClient.predict(features);

        double riskScore = riskResult.getRiskScore(); // 0=safe, 1=risky
        double trustScore = 1.0 - riskScore;          // 0=risky, 1=safe

        // ── Credit Tier ───────────────────────────────────────────────────
        String creditTier;
        boolean approved;
        double maxLoanMultiplier;
        double interestRatePercent;

        if (trustScore >= 0.80) {
            creditTier = "PLATINUM";
            approved = true;
            maxLoanMultiplier = 5.0;
            interestRatePercent = 3.5;
        } else if (trustScore >= 0.65) {
            creditTier = "GOLD";
            approved = true;
            maxLoanMultiplier = 3.0;
            interestRatePercent = 6.0;
        } else if (trustScore >= 0.50) {
            creditTier = "SILVER";
            approved = loanRequest.getAmount() <= 5000;
            maxLoanMultiplier = 1.5;
            interestRatePercent = 9.5;
        } else if (trustScore >= 0.35) {
            creditTier = "BRONZE";
            approved = loanRequest.getAmount() <= 1000;
            maxLoanMultiplier = 0.75;
            interestRatePercent = 14.0;
        } else {
            creditTier = "REJECTED";
            approved = false;
            maxLoanMultiplier = 0;
            interestRatePercent = 0;
        }

        double recommendedLimit = features.getTotalVolumeEth() > 0
                ? Math.min(features.getTotalVolumeEth() * maxLoanMultiplier * 2000, 100_000)
                : maxLoanMultiplier * 1000;

        // ── Loan reasons ──────────────────────────────────────────────────
        List<String> reasons = buildLoanReasons(features, riskResult, approved, creditTier, loanRequest.getAmount(), recommendedLimit);

        // ── Decision hash (on-chain proof) ────────────────────────────────
        String decisionPayload = loanRequest.getWalletAddress() + ":"
                + loanRequest.getAmount() + ":"
                + approved + ":"
                + riskResult.getRiskScore() + ":"
                + creditTier;

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("walletAddress", loanRequest.getWalletAddress());
        response.put("amount", loanRequest.getAmount());
        response.put("approved", approved);
        response.put("creditTier", creditTier);
        response.put("trustScore", Math.round(trustScore * 1000.0) / 1000.0);
        response.put("riskScore", Math.round(riskScore * 1000.0) / 1000.0);
        response.put("riskLevel", riskResult.getRiskLevel());
        response.put("interestRatePercent", approved ? interestRatePercent : null);
        response.put("recommendedLimit", approved ? Math.round(recommendedLimit) : 0);
        response.put("reasons", reasons);
        response.put("featureContributions", riskResult.getFeatureContributions());
        response.put("walletAgeDays", features.getWalletAgeDays());
        response.put("txCount", features.getTxCount());
        response.put("defiProtocolCount", features.getDefiProtocolCount());
        response.put("knownProtocols", features.getKnownProtocols());
        response.put("collateralRatio", Math.round(features.getCollateralRatio() * 100.0) / 100.0);
        response.put("liquidationEvents", features.getLiquidationEvents());
        response.put("flashLoanCount", features.getFlashLoanCount());
        response.put("decisionHash", sha256Hex(decisionPayload));
        response.put("timestamp", System.currentTimeMillis());

        return response;
    }

    private List<String> buildLoanReasons(WalletFeatures f, RiskResult risk, boolean approved,
                                          String tier, double requestedAmount, double recommendedLimit) {
        List<String> reasons = new ArrayList<>();

        // Positive factors
        if (f.getWalletAgeDays() >= 365) reasons.add("✅ Wallet active for over a year — strong history");
        else if (f.getWalletAgeDays() >= 90)  reasons.add("✅ Wallet age acceptable (≥90 days)");
        else reasons.add("❌ Wallet age below 90 days — insufficient history");

        if (f.getTxCount() >= 100) reasons.add("✅ High transaction count — active user");
        else if (f.getTxCount() >= 20) reasons.add("✅ Reasonable transaction count");
        else reasons.add("❌ Low transaction count — limited on-chain activity");

        if (f.getDefiProtocolCount() >= 3) reasons.add("✅ Active DeFi user across " + f.getDefiProtocolCount() + " protocols");
        else if (f.getDefiProtocolCount() > 0) reasons.add("⚠️ Limited DeFi protocol usage");

        if (f.getCollateralRatio() >= 1.5) reasons.add("✅ Healthy collateral ratio (" + String.format("%.2f", f.getCollateralRatio()) + "x)");
        else if (f.getCollateralRatio() >= 1.1) reasons.add("⚠️ Collateral ratio marginal (" + String.format("%.2f", f.getCollateralRatio()) + "x)");
        else reasons.add("❌ Insufficient collateral ratio (" + String.format("%.2f", f.getCollateralRatio()) + "x)");

        // Negative factors
        if (f.getLiquidationEvents() > 0)
            reasons.add("❌ " + f.getLiquidationEvents() + " past liquidation(s) — negative credit signal");

        if (f.getFlashLoanCount() > 3)
            reasons.add("❌ " + f.getFlashLoanCount() + " flash loans — high-risk behaviour pattern");
        else if (f.getFlashLoanCount() > 0)
            reasons.add("⚠️ " + f.getFlashLoanCount() + " flash loan(s) detected");

        if (f.getRugpullExposureScore() > 0.2)
            reasons.add("❌ High exposure to suspected rugpull contracts (" + String.format("%.0f%%", f.getRugpullExposureScore() * 100) + ")");

        if (f.getDormantPeriodDays() > 180)
            reasons.add("⚠️ Long wallet dormancy (" + String.format("%.0f", f.getDormantPeriodDays()) + " days)");

        // Amount vs limit
        if (approved && requestedAmount > recommendedLimit)
            reasons.add("⚠️ Requested amount exceeds recommended limit — partial approval may apply");

        if (!approved && "SILVER".equals(tier))
            reasons.add("❌ Requested amount exceeds SILVER tier limit ($5,000)");
        if (!approved && "BRONZE".equals(tier))
            reasons.add("❌ Requested amount exceeds BRONZE tier limit ($1,000)");

        // Denial reasons from ML model
        if (risk.getDenialReasons() != null) {
            for (String dr : risk.getDenialReasons()) {
                if (!dr.contains("meets all")) {
                    reasons.add("ℹ️ " + dr);
                }
            }
        }

        return reasons;
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
