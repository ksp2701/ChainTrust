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
        clean.setIncomingOutgoingRatio(Math.max(0.0, input.getIncomingOutgoingRatio()));
        clean.setTxVariance(Math.max(0.0, input.getTxVariance()));
        return clean;
    }

    public Map<String, Object> toMlPayload(WalletFeatures features) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("wallet_age_days", features.getWalletAgeDays());
        payload.put("tx_count", features.getTxCount());
        payload.put("avg_tx_value", features.getAvgTxValue());
        payload.put("unique_contracts", features.getUniqueContracts());
        payload.put("incoming_outgoing_ratio", features.getIncomingOutgoingRatio());
        payload.put("tx_variance", features.getTxVariance());
        return payload;
    }
}
