package com.chaintrust.service;

import com.chaintrust.model.TxRecord;
import com.chaintrust.model.WalletFeatures;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

@Service
public class WalletService {

    private static final Pattern ETH_ADDRESS = Pattern.compile("^0x[a-fA-F0-9]{40}$");

    private final WalletHistoryService historyService;

    public WalletService(WalletHistoryService historyService) {
        this.historyService = historyService;
    }

    public WalletFeatures extractFeatures(String address) {
        requireValidAddress(address);
        List<TxRecord> txs = historyService.fetchHistory(address);
        long firstTxTs  = historyService.fetchFirstTxTimestamp(address);
        long totalTxCnt = historyService.fetchTotalTxCount(address);
        return historyService.deriveFeatures(address, txs, firstTxTs, totalTxCnt);
    }

    public void requireValidAddress(String address) {
        if (address == null || !ETH_ADDRESS.matcher(address).matches()) {
            throw new IllegalArgumentException("Invalid EVM wallet address: " + address);
        }
    }
}
