package com.chaintrust.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class LoanOutcomeRequest {

    @NotBlank
    @Pattern(regexp = "^(0x)?[a-fA-F0-9]{64}$", message = "decisionHash must be 64-char hex (0x optional)")
    private String decisionHash;

    @NotBlank
    @Pattern(regexp = "^(?i)(REPAID|DEFAULTED)$", message = "outcome must be REPAID or DEFAULTED")
    private String outcome;

    public String getDecisionHash() {
        return decisionHash;
    }

    public void setDecisionHash(String decisionHash) {
        this.decisionHash = decisionHash;
    }

    public String getOutcome() {
        return outcome;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }
}
