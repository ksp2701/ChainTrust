package com.chaintrust.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public class LoanRequest {

    @NotBlank
    private String walletAddress;

    @Positive
    private double amount;

    public LoanRequest() {
    }

    public String getWalletAddress() {
        return walletAddress;
    }

    public void setWalletAddress(String walletAddress) {
        this.walletAddress = walletAddress;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }
}
