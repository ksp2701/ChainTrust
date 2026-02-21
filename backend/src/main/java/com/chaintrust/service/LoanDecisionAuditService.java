package com.chaintrust.service;

import com.chaintrust.model.LoanDecisionEntity;
import com.chaintrust.model.WalletFeatures;
import com.chaintrust.repository.LoanDecisionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class LoanDecisionAuditService {

    private static final List<String> LABELED_OUTCOMES = List.of("REPAID", "DEFAULTED");
    private final LoanDecisionRepository repository;
    private final FeatureService featureService;
    private final ObjectMapper objectMapper;

    public LoanDecisionAuditService(
            LoanDecisionRepository repository,
            FeatureService featureService,
            ObjectMapper objectMapper) {
        this.repository = repository;
        this.featureService = featureService;
        this.objectMapper = objectMapper;
    }

    public void persistDecision(
            String decisionHash,
            String walletAddress,
            double requestedAmount,
            WalletFeatures features,
            double trustScore,
            double riskScore,
            String riskLevel,
            boolean approved,
            String creditTier,
            double interestRatePercent,
            long recommendedLimit,
            List<String> reasons,
            String onChainStatus,
            String onChainTxHash,
            String onChainError) {
        LoanDecisionEntity entity = repository.findByDecisionHash(decisionHash).orElseGet(LoanDecisionEntity::new);
        entity.setDecisionHash(decisionHash);
        entity.setWalletAddress(walletAddress);
        entity.setRequestedAmount(requestedAmount);
        entity.setTrustScore(trustScore);
        entity.setRiskScore(riskScore);
        entity.setRiskLevel(riskLevel);
        entity.setApproved(approved);
        entity.setCreditTier(creditTier);
        entity.setInterestRatePercent(interestRatePercent);
        entity.setRecommendedLimit(recommendedLimit);
        entity.setOnChainStatus(onChainStatus);
        entity.setOnChainTxHash(onChainTxHash);
        entity.setOnChainError(onChainError);

        Map<String, Object> featuresPayload = featureService.toMlPayload(features);
        try {
            entity.setFeaturesJson(objectMapper.writeValueAsString(featuresPayload));
            entity.setReasonsJson(objectMapper.writeValueAsString(reasons != null ? reasons : List.of()));
        } catch (Exception ex) {
            entity.setFeaturesJson("{}");
            entity.setReasonsJson("[]");
        }

        repository.save(entity);
    }

    public Map<String, Object> updateOutcome(String decisionHash, String outcomeRaw) {
        String decisionHashLower = decisionHash.toLowerCase(Locale.ROOT);
        String outcome = outcomeRaw.toUpperCase(Locale.ROOT);
        Optional<LoanDecisionEntity> optional = repository.findByDecisionHash(decisionHashLower);
        if (optional.isEmpty()) {
            throw new IllegalArgumentException("Decision hash not found: " + decisionHashLower);
        }
        if (!LABELED_OUTCOMES.contains(outcome)) {
            throw new IllegalArgumentException("Unsupported outcome: " + outcome + ". Allowed: REPAID, DEFAULTED");
        }

        LoanDecisionEntity entity = optional.get();
        entity.setOutcomeLabel(outcome);
        entity.setOutcomeUpdatedAt(Instant.now());
        repository.save(entity);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("decisionHash", entity.getDecisionHash());
        response.put("outcome", entity.getOutcomeLabel());
        response.put("walletAddress", entity.getWalletAddress());
        response.put("updatedAt", entity.getOutcomeUpdatedAt());
        return response;
    }

    public List<Map<String, Object>> exportLabeledTrainingRows() {
        List<LoanDecisionEntity> entities = repository.findByOutcomeLabelIn(LABELED_OUTCOMES);
        List<Map<String, Object>> rows = new ArrayList<>();

        for (LoanDecisionEntity e : entities) {
            try {
                Map<String, Object> features = objectMapper.readValue(
                        e.getFeaturesJson(),
                        new TypeReference<Map<String, Object>>() {}
                );

                if (!hasAllTrainingFeatures(features)) {
                    continue;
                }

                Map<String, Object> row = new LinkedHashMap<>(features);
                row.put("label", "REPAID".equalsIgnoreCase(e.getOutcomeLabel()) ? 1 : 0);
                row.put("decision_hash", e.getDecisionHash());
                row.put("wallet_address", e.getWalletAddress());
                row.put("created_at", e.getCreatedAt() != null ? e.getCreatedAt().toString() : null);
                row.put("outcome_updated_at", e.getOutcomeUpdatedAt() != null ? e.getOutcomeUpdatedAt().toString() : null);
                rows.add(row);
            } catch (Exception ignored) {
            }
        }
        return rows;
    }

    private static boolean hasAllTrainingFeatures(Map<String, Object> row) {
        return row.containsKey("wallet_age_days")
                && row.containsKey("tx_count")
                && row.containsKey("avg_tx_value_eth")
                && row.containsKey("unique_contracts")
                && row.containsKey("incoming_outgoing_ratio")
                && row.containsKey("tx_variance")
                && row.containsKey("defi_protocol_count")
                && row.containsKey("flash_loan_count")
                && row.containsKey("liquidation_events")
                && row.containsKey("nft_transaction_count")
                && row.containsKey("max_single_tx_eth")
                && row.containsKey("dormant_period_days")
                && row.containsKey("collateral_ratio")
                && row.containsKey("cross_chain_count")
                && row.containsKey("rugpull_exposure_score");
    }
}
