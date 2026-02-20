package com.chaintrust.model;

<<<<<<< HEAD
import java.util.List;

public class LoanRequest {

    private String walletAddress;
    private double amount;
    private String collateralToken;    // e.g. "ETH", "WBTC"
    private double collateralAmount;   // in USD
    private int termDays;              // loan duration request
    private String purpose;           // "trading", "yield_farming", "leverage", "other"

    public LoanRequest() {}

    public String getWalletAddress() { return walletAddress; }
    public void setWalletAddress(String walletAddress) { this.walletAddress = walletAddress; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getCollateralToken() { return collateralToken; }
    public void setCollateralToken(String collateralToken) { this.collateralToken = collateralToken; }

    public double getCollateralAmount() { return collateralAmount; }
    public void setCollateralAmount(double collateralAmount) { this.collateralAmount = collateralAmount; }

    public int getTermDays() { return termDays; }
    public void setTermDays(int termDays) { this.termDays = termDays; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
=======
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
>>>>>>> e6bab9ff3e4c81f53c66b24db7e96dd1d61d97c1
}
