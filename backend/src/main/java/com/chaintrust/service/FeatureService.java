package com.chaintrust.service;

import com.chaintrust.model.WalletFeatures;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class FeatureService {

    private static final Pattern ETH_ADDRESS_PATTERN = Pattern.compile("^0x[a-fA-F0-9]{40}$");

    public void requireValidAddress(String address) {
        if (address == null || !ETH_ADDRESS_PATTERN.matcher(address).matches()) {
            throw new IllegalArgumentException("Invalid wallet address format");
        }
    }

    public WalletFeatures sanitize(WalletFeatures input) {
        WalletFeatures clean = new WalletFeatures();
        clean.setAddress(input.getAddress());
        clean.setWalletAgeDays(Math.max(0, input.getWalletAgeDays()));
        clean.setTxCount(Math.max(0, input.getTxCount()));
        clean.setAvgTxValue(Math.max(0.0, input.getAvgTxValue()));
        clean.setUniqueContracts(Math.max(0, input.getUniqueContracts()));
<<<<<<< HEAD
        clean.setIncomingOutgoingRatio(clamp(input.getIncomingOutgoingRatio(), 0, 1));
        clean.setTxVariance(Math.max(0.0, input.getTxVariance()));
        clean.setDefiProtocolCount(Math.max(0, input.getDefiProtocolCount()));
        clean.setFlashLoanCount(Math.max(0, input.getFlashLoanCount()));
        clean.setLiquidationEvents(Math.max(0, input.getLiquidationEvents()));
        clean.setNftTransactionCount(Math.max(0, input.getNftTransactionCount()));
        clean.setMaxSingleTxEth(Math.max(0.0, input.getMaxSingleTxEth()));
        clean.setDormantPeriodDays(Math.max(0.0, input.getDormantPeriodDays()));
        clean.setCollateralRatio(clamp(input.getCollateralRatio(), 0, 4));
        clean.setCrossChainCount(Math.max(0, input.getCrossChainCount()));
        clean.setRugpullExposureScore(clamp(input.getRugpullExposureScore(), 0, 1));
        clean.setFirstSeenDate(input.getFirstSeenDate());
        clean.setLastSeenDate(input.getLastSeenDate());
        clean.setTotalVolumeEth(Math.max(0, input.getTotalVolumeEth()));
        clean.setKnownProtocols(input.getKnownProtocols());
=======
        clean.setIncomingOutgoingRatio(Math.max(0.0, input.getIncomingOutgoingRatio()));
        clean.setTxVariance(Math.max(0.0, input.getTxVariance()));
>>>>>>> e6bab9ff3e4c81f53c66b24db7e96dd1d61d97c1
        return clean;
    }

    public Map<String, Object> toMlPayload(WalletFeatures features) {
        Map<String, Object> payload = new HashMap<>();
<<<<<<< HEAD
        // Core
        payload.put("wallet_age_days", features.getWalletAgeDays());
        payload.put("tx_count", features.getTxCount());
        payload.put("avg_tx_value_eth", features.getAvgTxValue());
        payload.put("unique_contracts", features.getUniqueContracts());
        payload.put("incoming_outgoing_ratio", features.getIncomingOutgoingRatio());
        payload.put("tx_variance", features.getTxVariance());
        // Extended DeFi
        payload.put("defi_protocol_count", features.getDefiProtocolCount());
        payload.put("flash_loan_count", features.getFlashLoanCount());
        payload.put("liquidation_events", features.getLiquidationEvents());
        payload.put("nft_transaction_count", features.getNftTransactionCount());
        payload.put("max_single_tx_eth", features.getMaxSingleTxEth());
        payload.put("dormant_period_days", features.getDormantPeriodDays());
        payload.put("collateral_ratio", features.getCollateralRatio());
        payload.put("cross_chain_count", features.getCrossChainCount());
        payload.put("rugpull_exposure_score", features.getRugpullExposureScore());
        return payload;
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
=======
        payload.put("wallet_age_days", features.getWalletAgeDays());
        payload.put("tx_count", features.getTxCount());
        payload.put("avg_tx_value", features.getAvgTxValue());
        payload.put("unique_contracts", features.getUniqueContracts());
        payload.put("incoming_outgoing_ratio", features.getIncomingOutgoingRatio());
        payload.put("tx_variance", features.getTxVariance());
        return payload;
    }
>>>>>>> e6bab9ff3e4c81f53c66b24db7e96dd1d61d97c1
}
