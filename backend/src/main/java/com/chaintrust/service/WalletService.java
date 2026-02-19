package com.chaintrust.service;

import com.chaintrust.model.WalletFeatures;
import org.springframework.stereotype.Service;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;

import java.math.BigInteger;

@Service
public class WalletService {

    private final Web3j web3j;
    private final FeatureService featureService;

    public WalletService(Web3j web3j, FeatureService featureService) {
        this.web3j = web3j;
        this.featureService = featureService;
    }

    public WalletFeatures extractFeatures(String address) {
        featureService.requireValidAddress(address);

        int txCount = fetchTxCount(address);
        int hash = Math.abs(address.toLowerCase().hashCode());

        WalletFeatures features = new WalletFeatures();
        features.setAddress(address);
        features.setTxCount(txCount);
        features.setWalletAgeDays(30L + (hash % 720L));
        features.setAvgTxValue(0.005 + ((hash % 150) / 1000.0));
        features.setUniqueContracts(1 + (hash % 20));
        features.setIncomingOutgoingRatio(0.25 + ((hash % 125) / 100.0));
        features.setTxVariance(0.05 + ((hash % 70) / 100.0));

        return featureService.sanitize(features);
    }

    private int fetchTxCount(String address) {
        try {
            BigInteger count = web3j.ethGetTransactionCount(address, DefaultBlockParameterName.LATEST)
                .send()
                .getTransactionCount();
            return count.min(BigInteger.valueOf(Integer.MAX_VALUE)).intValue();
        } catch (Exception e) {
            return 0;
        }
    }
}
