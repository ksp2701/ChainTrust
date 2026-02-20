package com.chaintrust.model;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;

public class WalletFeatures {

    @NotBlank
    private String address;

    // ── Core features ──────────────────────────────────────────────────────
    @Min(0) private long walletAgeDays;
    @Min(0) private int txCount;
    @DecimalMin("0.0") private double avgTxValue;
    @Min(0) private int uniqueContracts;
    @DecimalMin("0.0") private double incomingOutgoingRatio;
    @DecimalMin("0.0") private double txVariance;

    // ── Extended DeFi features ─────────────────────────────────────────────
    @Min(0) private int defiProtocolCount;
    @Min(0) private int flashLoanCount;
    @Min(0) private int liquidationEvents;
    @Min(0) private int nftTransactionCount;
    @DecimalMin("0.0") private double maxSingleTxEth;
    @DecimalMin("0.0") private double dormantPeriodDays;
    @DecimalMin("0.0") private double collateralRatio;
    @Min(0) private int crossChainCount;
    @DecimalMin("0.0") private double rugpullExposureScore;

    // ── Metadata ───────────────────────────────────────────────────────────
    private String firstSeenDate;
    private String lastSeenDate;
    private double totalVolumeEth;
    private List<String> knownProtocols;
    private Map<String, Double> featureContributions;

    public WalletFeatures() {}

    // ── Getters & Setters ──────────────────────────────────────────────────

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public long getWalletAgeDays() { return walletAgeDays; }
    public void setWalletAgeDays(long walletAgeDays) { this.walletAgeDays = walletAgeDays; }

    public int getTxCount() { return txCount; }
    public void setTxCount(int txCount) { this.txCount = txCount; }

    public double getAvgTxValue() { return avgTxValue; }
    public void setAvgTxValue(double avgTxValue) { this.avgTxValue = avgTxValue; }

    public int getUniqueContracts() { return uniqueContracts; }
    public void setUniqueContracts(int uniqueContracts) { this.uniqueContracts = uniqueContracts; }

    public double getIncomingOutgoingRatio() { return incomingOutgoingRatio; }
    public void setIncomingOutgoingRatio(double v) { this.incomingOutgoingRatio = v; }

    public double getTxVariance() { return txVariance; }
    public void setTxVariance(double txVariance) { this.txVariance = txVariance; }

    public int getDefiProtocolCount() { return defiProtocolCount; }
    public void setDefiProtocolCount(int v) { this.defiProtocolCount = v; }

    public int getFlashLoanCount() { return flashLoanCount; }
    public void setFlashLoanCount(int v) { this.flashLoanCount = v; }

    public int getLiquidationEvents() { return liquidationEvents; }
    public void setLiquidationEvents(int v) { this.liquidationEvents = v; }

    public int getNftTransactionCount() { return nftTransactionCount; }
    public void setNftTransactionCount(int v) { this.nftTransactionCount = v; }

    public double getMaxSingleTxEth() { return maxSingleTxEth; }
    public void setMaxSingleTxEth(double v) { this.maxSingleTxEth = v; }

    public double getDormantPeriodDays() { return dormantPeriodDays; }
    public void setDormantPeriodDays(double v) { this.dormantPeriodDays = v; }

    public double getCollateralRatio() { return collateralRatio; }
    public void setCollateralRatio(double v) { this.collateralRatio = v; }

    public int getCrossChainCount() { return crossChainCount; }
    public void setCrossChainCount(int v) { this.crossChainCount = v; }

    public double getRugpullExposureScore() { return rugpullExposureScore; }
    public void setRugpullExposureScore(double v) { this.rugpullExposureScore = v; }

    public String getFirstSeenDate() { return firstSeenDate; }
    public void setFirstSeenDate(String v) { this.firstSeenDate = v; }

    public String getLastSeenDate() { return lastSeenDate; }
    public void setLastSeenDate(String v) { this.lastSeenDate = v; }

    public double getTotalVolumeEth() { return totalVolumeEth; }
    public void setTotalVolumeEth(double v) { this.totalVolumeEth = v; }

    public List<String> getKnownProtocols() { return knownProtocols; }
    public void setKnownProtocols(List<String> v) { this.knownProtocols = v; }

    public Map<String, Double> getFeatureContributions() { return featureContributions; }
    public void setFeatureContributions(Map<String, Double> v) { this.featureContributions = v; }
}
