package com.chaintrust.controller;

import com.chaintrust.model.TxRecord;
import com.chaintrust.service.WalletHistoryService;
import com.chaintrust.service.WalletService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/wallet")
@CrossOrigin(origins = "*")
public class WalletHistoryController {

    private final WalletHistoryService historyService;
    private final WalletService walletService;

    public WalletHistoryController(WalletHistoryService historyService, WalletService walletService) {
        this.historyService = historyService;
        this.walletService = walletService;
    }

    @GetMapping("/{address}/history")
    public ResponseEntity<List<TxRecord>> history(@PathVariable String address) {
        try {
            walletService.requireValidAddress(address);
            List<TxRecord> txs = historyService.fetchHistory(address);
            return ResponseEntity.ok(txs);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }
}
