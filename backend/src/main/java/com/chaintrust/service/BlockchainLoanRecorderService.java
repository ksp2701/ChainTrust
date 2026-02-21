package com.chaintrust.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint32;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.response.PollingTransactionReceiptProcessor;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;

@Service
public class BlockchainLoanRecorderService {

    private final Web3j web3j;
    private final boolean enabled;
    private final boolean required;
    private final long chainId;
    private final String contractAddress;
    private final String privateKey;
    private final BigInteger gasLimit;
    private final BigInteger gasPriceWei;

    public BlockchainLoanRecorderService(
            Web3j web3j,
            @Value("${blockchain.enabled:false}") boolean enabled,
            @Value("${blockchain.required:false}") boolean required,
            @Value("${blockchain.chain-id:11155111}") long chainId,
            @Value("${blockchain.contract-address:}") String contractAddress,
            @Value("${blockchain.private-key:}") String privateKey,
            @Value("${blockchain.gas-limit:550000}") long gasLimit,
            @Value("${blockchain.gas-price-wei:0}") long gasPriceWei) {
        this.web3j = web3j;
        this.enabled = enabled;
        this.required = required;
        this.chainId = chainId;
        this.contractAddress = contractAddress != null ? contractAddress.trim() : "";
        this.privateKey = privateKey != null ? privateKey.trim() : "";
        this.gasLimit = BigInteger.valueOf(Math.max(21000L, gasLimit));
        this.gasPriceWei = BigInteger.valueOf(Math.max(0L, gasPriceWei));
    }

    public ChainWriteResult recordLoanDecision(
            String walletAddress,
            double amountUsd,
            double riskScore,
            String creditTier,
            boolean approved,
            String decisionHashHex,
            String purpose) {
        if (!enabled) {
            return ChainWriteResult.disabled("BLOCKCHAIN_DISABLED");
        }

        if (!isConfigured()) {
            String msg = "Blockchain config missing or invalid (contract address/private key)";
            if (required) {
                throw new IllegalStateException(msg);
            }
            return ChainWriteResult.failed(false, msg);
        }

        try {
            Credentials credentials = Credentials.create(normalizePrivateKey(privateKey));
            RawTransactionManager txManager = new RawTransactionManager(web3j, credentials, chainId);

            Function function = new Function(
                    "recordLoanDecision",
                    Arrays.asList(
                            new Address(walletAddress),
                            new Uint256(toUsdCents(amountUsd)),
                            new Uint32(BigInteger.valueOf(toRiskBps(riskScore))),
                            new Bool(approved),
                            new Uint8(BigInteger.valueOf(toTierEnumValue(creditTier))),
                            new Bytes32(toBytes32(decisionHashHex)),
                            new Utf8String(purpose != null ? purpose : "")
                    ),
                    Collections.emptyList()
            );

            String data = FunctionEncoder.encode(function);
            BigInteger gasPrice = resolveGasPrice();
            EthSendTransaction tx = txManager.sendTransaction(gasPrice, gasLimit, contractAddress, data, BigInteger.ZERO);

            if (tx == null) {
                String msg = "Null response from eth_sendRawTransaction";
                if (required) {
                    throw new IllegalStateException(msg);
                }
                return ChainWriteResult.failed(true, msg);
            }

            if (tx.hasError()) {
                String msg = tx.getError().getMessage();
                if (required) {
                    throw new IllegalStateException(msg);
                }
                return ChainWriteResult.failed(true, msg);
            }

            String txHash = tx.getTransactionHash();
            TransactionReceipt receipt = new PollingTransactionReceiptProcessor(web3j, 1500, 40)
                    .waitForTransactionReceipt(txHash);

            if (receipt == null) {
                return ChainWriteResult.submitted(txHash);
            }

            if (!"0x1".equalsIgnoreCase(receipt.getStatus())) {
                String msg = "On-chain transaction reverted";
                if (required) {
                    throw new IllegalStateException(msg);
                }
                return ChainWriteResult.failed(true, msg, txHash);
            }

            return ChainWriteResult.confirmed(txHash);
        } catch (Exception ex) {
            if (required) {
                throw new IllegalStateException("Failed to record on-chain decision: " + ex.getMessage(), ex);
            }
            return ChainWriteResult.failed(true, ex.getMessage());
        }
    }

    private BigInteger resolveGasPrice() throws Exception {
        if (gasPriceWei.signum() > 0) {
            return gasPriceWei;
        }
        return web3j.ethGasPrice().send().getGasPrice();
    }

    private boolean isConfigured() {
        return WalletUtils.isValidAddress(contractAddress) && isValidPrivateKey(privateKey);
    }

    private static boolean isValidPrivateKey(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        String normalized = normalizePrivateKey(value);
        return normalized.matches("^[0-9a-fA-F]{64}$");
    }

    private static String normalizePrivateKey(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim();
        if (normalized.startsWith("0x") || normalized.startsWith("0X")) {
            normalized = normalized.substring(2);
        }
        return normalized;
    }

    private static BigInteger toUsdCents(double amountUsd) {
        return BigDecimal.valueOf(Math.max(0.0, amountUsd))
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .toBigInteger();
    }

    private static int toRiskBps(double riskScore) {
        double clamped = Math.max(0.0, Math.min(1.0, riskScore));
        return (int) Math.round(clamped * 10_000.0);
    }

    private static int toTierEnumValue(String tier) {
        if (tier == null) {
            return 0;
        }
        return switch (tier.toUpperCase(Locale.ROOT)) {
            case "BRONZE" -> 1;
            case "SILVER" -> 2;
            case "GOLD" -> 3;
            case "PLATINUM" -> 4;
            default -> 0;
        };
    }

    private static byte[] toBytes32(String hexValue) {
        String normalized = hexValue == null ? "" : hexValue.trim();
        if (normalized.startsWith("0x") || normalized.startsWith("0X")) {
            normalized = normalized.substring(2);
        }
        if (!normalized.matches("^[0-9a-fA-F]{64}$")) {
            throw new IllegalArgumentException("decisionHash must be 32-byte hex string");
        }
        return Numeric.hexStringToByteArray("0x" + normalized);
    }

    public static final class ChainWriteResult {
        private final boolean configured;
        private final boolean submitted;
        private final boolean confirmed;
        private final String status;
        private final String txHash;
        private final String error;

        private ChainWriteResult(boolean configured, boolean submitted, boolean confirmed, String status, String txHash, String error) {
            this.configured = configured;
            this.submitted = submitted;
            this.confirmed = confirmed;
            this.status = status;
            this.txHash = txHash;
            this.error = error;
        }

        public static ChainWriteResult disabled(String status) {
            return new ChainWriteResult(false, false, false, status, null, null);
        }

        public static ChainWriteResult failed(boolean configured, String error) {
            return new ChainWriteResult(configured, false, false, "FAILED", null, error);
        }

        public static ChainWriteResult failed(boolean configured, String error, String txHash) {
            return new ChainWriteResult(configured, true, false, "FAILED", txHash, error);
        }

        public static ChainWriteResult submitted(String txHash) {
            return new ChainWriteResult(true, true, false, "SUBMITTED", txHash, null);
        }

        public static ChainWriteResult confirmed(String txHash) {
            return new ChainWriteResult(true, true, true, "CONFIRMED", txHash, null);
        }

        public boolean isConfigured() {
            return configured;
        }

        public boolean isSubmitted() {
            return submitted;
        }

        public boolean isConfirmed() {
            return confirmed;
        }

        public String getStatus() {
            return status;
        }

        public String getTxHash() {
            return txHash;
        }

        public String getError() {
            return error;
        }
    }
}
