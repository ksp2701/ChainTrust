package com.chaintrust.model;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class WalletFeatures {

    @NotBlank
    private String address;

    @Min(0)
    private long walletAgeDays;

    @Min(0)
    private int txCount;

    @DecimalMin("0.0")
    private double avgTxValue;

    @Min(0)
    private int uniqueContracts;

    @DecimalMin("0.0")
    private double incomingOutgoingRatio;

    @DecimalMin("0.0")
    private double txVariance;

    public WalletFeatures() {
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public long getWalletAgeDays() {
        return walletAgeDays;
    }

    public void setWalletAgeDays(long walletAgeDays) {
        this.walletAgeDays = walletAgeDays;
    }

    public int getTxCount() {
        return txCount;
    }

    public void setTxCount(int txCount) {
        this.txCount = txCount;
    }

    public double getAvgTxValue() {
        return avgTxValue;
    }

    public void setAvgTxValue(double avgTxValue) {
        this.avgTxValue = avgTxValue;
    }

    public int getUniqueContracts() {
        return uniqueContracts;
    }

    public void setUniqueContracts(int uniqueContracts) {
        this.uniqueContracts = uniqueContracts;
    }

    public double getIncomingOutgoingRatio() {
        return incomingOutgoingRatio;
    }

    public void setIncomingOutgoingRatio(double incomingOutgoingRatio) {
        this.incomingOutgoingRatio = incomingOutgoingRatio;
    }

    public double getTxVariance() {
        return txVariance;
    }

    public void setTxVariance(double txVariance) {
        this.txVariance = txVariance;
    }
}
