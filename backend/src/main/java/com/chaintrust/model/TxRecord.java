package com.chaintrust.model;

public class TxRecord {
    private String hash;
    private long blockNumber;
    private long timestamp;
    private String from;
    private String to;
    private double valueEth;
    private boolean isContract;
    private String methodId;
    private String protocol;  // e.g. "Uniswap", "Aave", "Unknown"
    private String riskFlag;  // "NORMAL", "FLASH_LOAN", "LIQUIDATION", "RUGPULL", "NFT"

    public TxRecord() {}

    public TxRecord(String hash, long blockNumber, long timestamp,
                    String from, String to, double valueEth,
                    boolean isContract, String methodId, String protocol, String riskFlag) {
        this.hash = hash;
        this.blockNumber = blockNumber;
        this.timestamp = timestamp;
        this.from = from;
        this.to = to;
        this.valueEth = valueEth;
        this.isContract = isContract;
        this.methodId = methodId;
        this.protocol = protocol;
        this.riskFlag = riskFlag;
    }

    public String getHash() { return hash; }
    public void setHash(String hash) { this.hash = hash; }

    public long getBlockNumber() { return blockNumber; }
    public void setBlockNumber(long blockNumber) { this.blockNumber = blockNumber; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }

    public String getTo() { return to; }
    public void setTo(String to) { this.to = to; }

    public double getValueEth() { return valueEth; }
    public void setValueEth(double valueEth) { this.valueEth = valueEth; }

    public boolean isContract() { return isContract; }
    public void setContract(boolean contract) { isContract = contract; }

    public String getMethodId() { return methodId; }
    public void setMethodId(String methodId) { this.methodId = methodId; }

    public String getProtocol() { return protocol; }
    public void setProtocol(String protocol) { this.protocol = protocol; }

    public String getRiskFlag() { return riskFlag; }
    public void setRiskFlag(String riskFlag) { this.riskFlag = riskFlag; }
}
