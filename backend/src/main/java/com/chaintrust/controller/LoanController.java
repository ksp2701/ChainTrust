package com.chaintrust.controller;

import com.chaintrust.model.LoanRequest;
import com.chaintrust.model.LoanOutcomeRequest;
import com.chaintrust.service.LoanDecisionAuditService;
import com.chaintrust.service.LoanService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/loan")
@CrossOrigin(origins = "*")
public class LoanController {

    private final LoanService loanService;
    private final LoanDecisionAuditService loanDecisionAuditService;

    public LoanController(LoanService loanService, LoanDecisionAuditService loanDecisionAuditService) {
        this.loanService = loanService;
        this.loanDecisionAuditService = loanDecisionAuditService;
    }

    @PostMapping("/evaluate")
    public ResponseEntity<Map<String, Object>> evaluate(@Valid @RequestBody LoanRequest request) {
        return ResponseEntity.ok(loanService.evaluate(request));
    }

    @PostMapping("/outcome")
    public ResponseEntity<Map<String, Object>> updateOutcome(@Valid @RequestBody LoanOutcomeRequest request) {
        try {
            String normalizedHash = request.getDecisionHash().toLowerCase();
            if (normalizedHash.startsWith("0x")) {
                normalizedHash = normalizedHash.substring(2);
            }
            return ResponseEntity.ok(
                    loanDecisionAuditService.updateOutcome(
                            normalizedHash,
                            request.getOutcome()
                    )
            );
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @GetMapping("/training-data")
    public ResponseEntity<List<Map<String, Object>>> exportTrainingData() {
        return ResponseEntity.ok(loanDecisionAuditService.exportLabeledTrainingRows());
    }
}
