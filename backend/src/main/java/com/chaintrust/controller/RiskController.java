package com.chaintrust.controller;

import com.chaintrust.model.RiskResult;
import com.chaintrust.model.WalletFeatures;
import com.chaintrust.service.RiskServiceClient;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/risk")
@CrossOrigin(origins = "*")
public class RiskController {

    private final RiskServiceClient riskServiceClient;

    public RiskController(RiskServiceClient riskServiceClient) {
        this.riskServiceClient = riskServiceClient;
    }

    @PostMapping
    public ResponseEntity<RiskResult> score(@Valid @RequestBody WalletFeatures features) {
        return ResponseEntity.ok(riskServiceClient.predict(features));
    }
}
