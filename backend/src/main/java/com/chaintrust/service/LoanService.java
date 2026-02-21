package com.chaintrust.service;

import com.chaintrust.model.LoanRequest;
import com.chaintrust.model.RiskResult;
import com.chaintrust.model.WalletFeatures;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class LoanService {

    private final FeatureService featureService;
    private final WalletService walletService;
    private final RiskServiceClient riskServiceClient;
    private final AddressIntelligenceService addressIntelligenceService;
    private final LoanPolicyService loanPolicyService;
    private final BlockchainLoanRecorderService blockchainLoanRecorderService;
    private final LoanDecisionAuditService loanDecisionAuditService;

    public LoanService(
            FeatureService featureService,
            WalletService walletService,
            RiskServiceClient riskServiceClient,
            AddressIntelligenceService addressIntelligenceService,
            LoanPolicyService loanPolicyService,
            BlockchainLoanRecorderService blockchainLoanRecorderService,
            LoanDecisionAuditService loanDecisionAuditService) {
        this.featureService = featureService;
        this.walletService = walletService;
        this.riskServiceClient = riskServiceClient;
        this.addressIntelligenceService = addressIntelligenceService;
        this.loanPolicyService = loanPolicyService;
        this.blockchainLoanRecorderService = blockchainLoanRecorderService;
        this.loanDecisionAuditService = loanDecisionAuditService;
    }

    public Map<String, Object> evaluate(LoanRequest loanRequest) {
        featureService.requireValidAddress(loanRequest.getWalletAddress());

        WalletFeatures features = walletService.extractFeatures(loanRequest.getWalletAddress());
        RiskResult riskResult = riskServiceClient.predict(features);
        AddressIntelligenceService.AddressAssessment addressAssessment = addressIntelligenceService.assess(loanRequest.getWalletAddress());

        LoanPolicyService.PolicyDecision policyDecision = loanPolicyService.evaluate(
                loanRequest,
                features,
                riskResult,
                addressAssessment
        );

        String decisionPayload = loanRequest.getWalletAddress() + ":"
                + loanRequest.getAmount() + ":"
                + policyDecision.approved() + ":"
                + policyDecision.riskScore() + ":"
                + policyDecision.creditTier();
        String decisionHash = sha256Hex(decisionPayload);

        BlockchainLoanRecorderService.ChainWriteResult chainWrite = blockchainLoanRecorderService.recordLoanDecision(
                loanRequest.getWalletAddress(),
                loanRequest.getAmount(),
                policyDecision.riskScore(),
                policyDecision.creditTier(),
                policyDecision.approved(),
                decisionHash,
                loanRequest.getPurpose()
        );

        List<String> reasons = buildReasons(loanRequest, features, riskResult, policyDecision, addressAssessment);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("walletAddress", loanRequest.getWalletAddress());
        response.put("amount", loanRequest.getAmount());
        response.put("approved", policyDecision.approved());
        response.put("creditTier", policyDecision.creditTier());
        response.put("trustScore", round3(policyDecision.trustScore()));
        response.put("riskScore", round3(policyDecision.riskScore()));
        response.put("riskLevel", riskResult.getRiskLevel());
        response.put("interestRatePercent", policyDecision.approved() ? policyDecision.interestRatePercent() : null);
        response.put("recommendedLimit", policyDecision.approved() ? policyDecision.recommendedLimit() : 0);
        response.put("reasons", reasons);
        response.put("featureContributions", riskResult.getFeatureContributions());
        response.put("walletAgeDays", features.getWalletAgeDays());
        response.put("txCount", features.getTxCount());
        response.put("defiProtocolCount", features.getDefiProtocolCount());
        response.put("knownProtocols", features.getKnownProtocols());
        response.put("collateralRatio", Math.round(features.getCollateralRatio() * 100.0) / 100.0);
        response.put("liquidationEvents", features.getLiquidationEvents());
        response.put("flashLoanCount", features.getFlashLoanCount());
        response.put("decisionHash", decisionHash);
        response.put("timestamp", System.currentTimeMillis());

        response.put("addressBurn", addressAssessment.burnAddress());
        response.put("addressKnownProtocolContract", addressAssessment.knownProtocolContract());
        response.put("addressSmartContract", addressAssessment.smartContract());
        response.put("addressContractCheckSucceeded", addressAssessment.contractCheckSucceeded());
        response.put("addressContractCheckError", addressAssessment.contractCheckError());
        response.put("policyHardRejected", policyDecision.hardRejected());
        response.put("policyReasons", policyDecision.policyReasons());

        response.put("onChainStatus", chainWrite.getStatus());
        response.put("onChainConfigured", chainWrite.isConfigured());
        response.put("onChainSubmitted", chainWrite.isSubmitted());
        response.put("onChainConfirmed", chainWrite.isConfirmed());
        response.put("onChainTxHash", chainWrite.getTxHash());
        response.put("onChainError", chainWrite.getError());

        loanDecisionAuditService.persistDecision(
                decisionHash,
                loanRequest.getWalletAddress(),
                loanRequest.getAmount(),
                features,
                policyDecision.trustScore(),
                policyDecision.riskScore(),
                riskResult.getRiskLevel(),
                policyDecision.approved(),
                policyDecision.creditTier(),
                policyDecision.interestRatePercent(),
                policyDecision.recommendedLimit(),
                reasons,
                chainWrite.getStatus(),
                chainWrite.getTxHash(),
                chainWrite.getError()
        );

        return response;
    }

    private List<String> buildReasons(
            LoanRequest request,
            WalletFeatures features,
            RiskResult riskResult,
            LoanPolicyService.PolicyDecision policyDecision,
            AddressIntelligenceService.AddressAssessment addressAssessment) {
        List<String> reasons = new ArrayList<>();

        reasons.addAll(policyDecision.policyReasons());

        if (features.getWalletAgeDays() >= 365) {
            reasons.add("Wallet active for over a year");
        } else if (features.getWalletAgeDays() >= 90) {
            reasons.add("Wallet age is acceptable (>= 90 days)");
        } else {
            reasons.add("Wallet age below 90 days");
        }

        if (features.getTxCount() >= 100) {
            reasons.add("High transaction count");
        } else if (features.getTxCount() >= 20) {
            reasons.add("Reasonable transaction count");
        } else {
            reasons.add("Low transaction count");
        }

        if (features.getDefiProtocolCount() >= 3) {
            reasons.add("Active DeFi usage across " + features.getDefiProtocolCount() + " protocols");
        } else if (features.getDefiProtocolCount() > 0) {
            reasons.add("Limited DeFi protocol usage");
        }

        if (features.getCollateralRatio() >= 1.5) {
            reasons.add("Healthy collateral ratio (" + String.format("%.2f", features.getCollateralRatio()) + "x)");
        } else if (features.getCollateralRatio() >= 1.1) {
            reasons.add("Collateral ratio is marginal (" + String.format("%.2f", features.getCollateralRatio()) + "x)");
        } else {
            reasons.add("Insufficient collateral ratio (" + String.format("%.2f", features.getCollateralRatio()) + "x)");
        }

        if (features.getLiquidationEvents() > 0) {
            reasons.add(features.getLiquidationEvents() + " past liquidation(s)");
        }

        if (features.getFlashLoanCount() > 3) {
            reasons.add(features.getFlashLoanCount() + " flash loans indicate high-risk behavior");
        } else if (features.getFlashLoanCount() > 0) {
            reasons.add(features.getFlashLoanCount() + " flash loan(s) detected");
        }

        if (features.getRugpullExposureScore() > 0.2) {
            reasons.add("High rugpull exposure (" + String.format("%.0f%%", features.getRugpullExposureScore() * 100) + ")");
        }

        if (features.getDormantPeriodDays() > 180) {
            reasons.add("Long dormancy period (" + String.format("%.0f", features.getDormantPeriodDays()) + " days)");
        }

        if (policyDecision.approved() && request.getAmount() > policyDecision.recommendedLimit()) {
            reasons.add("Requested amount exceeds recommended limit");
        }

        if (addressAssessment.burnAddress()) {
            reasons.add("Address flagged as burn/null wallet");
        }
        if (addressAssessment.knownProtocolContract()) {
            reasons.add("Address flagged as known protocol/router contract");
        }
        if (addressAssessment.smartContract()) {
            reasons.add("Address detected as smart contract");
        }

        if (riskResult.getDenialReasons() != null) {
            for (String denialReason : riskResult.getDenialReasons()) {
                if (denialReason == null || denialReason.isBlank()) {
                    continue;
                }
                if (!denialReason.toLowerCase().contains("meets all")) {
                    reasons.add("ML: " + denialReason);
                }
            }
        }

        return reasons;
    }

    private static double round3(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}