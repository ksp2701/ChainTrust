package com.chaintrust.service;

import com.chaintrust.model.LoanRequest;
import com.chaintrust.model.RiskResult;
import com.chaintrust.model.WalletFeatures;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;

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
        boolean approved = riskResult.getRiskScore() < 0.60;

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("walletAddress", loanRequest.getWalletAddress());
        response.put("amount", loanRequest.getAmount());
        response.put("riskScore", riskResult.getRiskScore());
        response.put("riskLevel", riskResult.getRiskLevel());
        response.put("approved", approved);

        String decisionPayload = loanRequest.getWalletAddress() + ":" + loanRequest.getAmount() + ":" + approved + ":" + riskResult.getRiskScore();
        response.put("decisionHash", sha256Hex(decisionPayload));

        return response;
    }

    private String sha256Hex(String value) {
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
