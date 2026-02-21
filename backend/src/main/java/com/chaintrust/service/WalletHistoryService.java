package com.chaintrust.service;

import com.chaintrust.model.TxRecord;
import com.chaintrust.model.WalletFeatures;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Fetches wallet transactions from Etherscan and derives model features.
 * Synthetic history is optional and disabled by default for real mode.
 */
@Service
public class WalletHistoryService {

    private static final double WEI_TO_ETH = 1e-18;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    private static final Map<String, String> PROTOCOL_MAP = Map.of(
        "0x7a250d5630b4cf539739df2c5dacb4c659f2488d", "Uniswap V2",
        "0xe592427a0aece92de3edee1f18e0157c05861564", "Uniswap V3",
        "0x7fc66500c84a76ad7e9c93437bfc5ac33e2ddae9", "Aave",
        "0x3d9819210a31b4961b30ef54be2aed79b9c9cd3b", "Compound",
        "0xd9e1ce17f2641f24ae83637ab66a2cca9c378b9f", "SushiSwap",
        "0x68b3465833fb72a70ecdf485e0e4c7bd8665fc45", "Uniswap Universal Router",
        "0x1111111254fb6c44bac0bed2854e76f90643097d", "1inch",
        "0x00000000219ab540356cbb839cbe05303d7705fa", "ETH2 Deposit",
        "0xc02aaa39b223fe8d0a0e5c4f27ead9083c756cc2", "WETH",
        "0xa0b86991c6218b36c1d19d4a2e9eb0ce3606eb48", "USDC"
    );

    private static final Set<String> FLASH_LOAN_SIGNATURES = Set.of(
        "0x5cffe9de", "0xab9c4b5d", "0x1b11d0b4"
    );

    private static final Set<String> NFT_CONTRACTS = Set.of(
        "0xbc4ca0eda7647a8ab7c2061c2e118a18a936f13d",
        "0x60e4d786628fea6478f785a6d7e704777c86a7c6",
        "0x23581767a106ae21c074b2276d25e5c3e136a68b"
    );

    private final RestTemplate restTemplate;
    private final List<String> apiKeys;
    private final AtomicInteger keyIndex = new AtomicInteger(0);
    private final long etherscanChainId;
    private final boolean syntheticFallbackEnabled;

    public WalletHistoryService(
            RestTemplateBuilder builder,
            @Value("${etherscan.api-keys:}") String apiKeysCsv,
            @Value("${etherscan.chain-id:1}") long etherscanChainId,
            @Value("${wallet.synthetic-fallback-enabled:false}") boolean syntheticFallbackEnabled) {
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(10))
                .build();
        this.apiKeys = Arrays.stream(apiKeysCsv.split(","))
                .map(String::trim)
                .filter(k -> !k.isBlank())
                .toList();
        this.etherscanChainId = etherscanChainId;
        this.syntheticFallbackEnabled = syntheticFallbackEnabled;
    }

    public List<TxRecord> fetchHistory(String address) {
        if (apiKeys.isEmpty()) {
            if (syntheticFallbackEnabled) {
                return buildSyntheticHistory(address);
            }
            throw new IllegalStateException("ETHERSCAN_API_KEYS is missing and synthetic fallback is disabled");
        }

        try {
            return fetchFromEtherscan(address);
        } catch (RuntimeException ex) {
            if (syntheticFallbackEnabled) {
                return buildSyntheticHistory(address);
            }
            throw ex;
        }
    }

    /**
     * Fetches the timestamp of the very first transaction ever made by this wallet.
     * Uses sort=asc&offset=1 so we only need 1 record regardless of total tx count.
     * Returns -1 if unavailable.
     */
    public long fetchFirstTxTimestamp(String address) {
        if (apiKeys.isEmpty()) return -1;
        for (String key : orderedApiKeys()) {
            try {
                String url = "https://api.etherscan.io/v2/api"
                        + "?chainid=" + etherscanChainId
                        + "&module=account&action=txlist"
                        + "&address=" + address
                        + "&startblock=0&endblock=99999999"
                        + "&page=1&offset=1&sort=asc"
                        + "&apikey=" + key;
                Map<String, Object> resp = restTemplate.getForObject(url, Map.class);
                if (resp == null) continue;
                Object resultObj = resp.get("result");
                if ("1".equals(str(resp.get("status"))) && resultObj instanceof List<?> rawList && !((List<?>) rawList).isEmpty()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> first = (Map<String, Object>) ((List<?>) rawList).get(0);
                    long ts = parseLong(first.get("timeStamp"));
                    if (ts > 0) return ts;
                }
            } catch (Exception ignored) {}
        }
        return -1;
    }

    /**
     * Fetches the total outgoing transaction count (nonce) via eth_getTransactionCount.
     * This is the true total tx count for an EOA wallet, regardless of the 100-tx fetch limit.
     * Returns -1 if unavailable.
     */
    public long fetchTotalTxCount(String address) {
        if (apiKeys.isEmpty()) return -1;
        for (String key : orderedApiKeys()) {
            try {
                String url = "https://api.etherscan.io/v2/api"
                        + "?chainid=" + etherscanChainId
                        + "&module=proxy&action=eth_getTransactionCount"
                        + "&address=" + address
                        + "&tag=latest"
                        + "&apikey=" + key;
                Map<String, Object> resp = restTemplate.getForObject(url, Map.class);
                if (resp == null) continue;
                String hexResult = str(resp.get("result"));
                if (hexResult != null && hexResult.startsWith("0x")) {
                    long count = Long.parseLong(hexResult.substring(2), 16);
                    if (count >= 0) return count;
                }
            } catch (Exception ignored) {}
        }
        return -1;
    }

    public WalletFeatures deriveFeatures(String address, List<TxRecord> txs) {
        return deriveFeatures(address, txs, -1, -1);
    }

    /**
     * Derives ML features from the fetched transaction list.
     *
     * @param firstTxTimestamp  true first-ever tx timestamp from fetchFirstTxTimestamp() — used for accurate wallet age.
     *                          Pass -1 to fall back to the oldest tx in the list (may undercount for active wallets).
     * @param totalTxCount      true total tx count from fetchTotalTxCount() — used instead of list size.
     *                          Pass -1 to fall back to list size.
     */
    public WalletFeatures deriveFeatures(String address, List<TxRecord> txs, long firstTxTimestamp, long totalTxCount) {
        WalletFeatures f = new WalletFeatures();
        f.setAddress(address);

        if (txs.isEmpty()) {
            f.setWalletAgeDays(1);
            f.setCollateralRatio(1.5); // neutral default — avoids triggering the <1.2 denial threshold
            return f;
        }

        long minTs = txs.stream().mapToLong(TxRecord::getTimestamp).min().orElse(Instant.now().getEpochSecond());
        long maxTs = txs.stream().mapToLong(TxRecord::getTimestamp).max().orElse(Instant.now().getEpochSecond());
        long nowTs = Instant.now().getEpochSecond();

        // Use the true first-tx timestamp if available (more accurate for wallets with >100 txs)
        long ageBaseTs = (firstTxTimestamp > 0) ? firstTxTimestamp : minTs;
        long walletAgeDays = Math.max(1, (nowTs - ageBaseTs) / 86400);
        f.setWalletAgeDays(walletAgeDays);

        // Use true total tx count if available, else fall back to list size
        long effectiveTxCount = (totalTxCount > 0) ? totalTxCount : txs.size();
        f.setTxCount((int) Math.min(effectiveTxCount, Integer.MAX_VALUE));
        f.setFirstSeenDate(LocalDate.ofInstant(Instant.ofEpochSecond(minTs), ZoneOffset.UTC).format(DATE_FMT));
        f.setLastSeenDate(LocalDate.ofInstant(Instant.ofEpochSecond(maxTs), ZoneOffset.UTC).format(DATE_FMT));

        double[] values = txs.stream().mapToDouble(TxRecord::getValueEth).toArray();
        double avg = Arrays.stream(values).average().orElse(0);
        double max = Arrays.stream(values).max().orElse(0);
        double variance = Arrays.stream(values).map(v -> (v - avg) * (v - avg)).average().orElse(0);
        double totalVolume = Arrays.stream(values).sum();

        f.setAvgTxValue(avg);
        f.setMaxSingleTxEth(max);
        f.setTxVariance(Math.sqrt(variance));
        f.setTotalVolumeEth(totalVolume);

        long incoming = txs.stream().filter(t -> t.getTo() != null && t.getTo().equalsIgnoreCase(address)).count();
        long outgoing = txs.stream().filter(t -> t.getFrom() != null && t.getFrom().equalsIgnoreCase(address)).count();
        long totalDirectional = incoming + outgoing;
        f.setIncomingOutgoingRatio(totalDirectional == 0 ? 0.0 : (double) incoming / totalDirectional);

        Set<String> contracts = txs.stream()
                .filter(TxRecord::isContract)
                .map(t -> t.getTo() != null ? t.getTo().toLowerCase(Locale.ROOT) : "")
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
        f.setUniqueContracts(contracts.size());

        Set<String> protocolsUsed = txs.stream()
                .map(TxRecord::getProtocol)
                .filter(p -> p != null && !p.equals("Unknown") && !p.equals("ETH Transfer"))
                .collect(Collectors.toSet());
        f.setDefiProtocolCount(protocolsUsed.size());
        f.setKnownProtocols(new ArrayList<>(protocolsUsed));

        f.setFlashLoanCount((int) txs.stream().filter(t -> "FLASH_LOAN".equals(t.getRiskFlag())).count());
        f.setLiquidationEvents((int) txs.stream().filter(t -> "LIQUIDATION".equals(t.getRiskFlag())).count());
        f.setNftTransactionCount((int) txs.stream().filter(t -> "NFT".equals(t.getRiskFlag())).count());
        f.setCrossChainCount(estimateCrossChainCount(txs));
        f.setDormantPeriodDays(computeMaxDormantDays(txs));

        double collateral = 1.5 + (protocolsUsed.size() * 0.15) - (f.getFlashLoanCount() * 0.3)
                - (f.getLiquidationEvents() * 0.5);
        f.setCollateralRatio(Math.max(0.1, Math.min(4.0, collateral)));

        long rugExposure = txs.stream().filter(t -> "RUGPULL".equals(t.getRiskFlag())).count();
        f.setRugpullExposureScore(Math.min(1.0, (double) rugExposure / Math.max(1, txs.size())));
        return f;
    }

    private List<TxRecord> fetchFromEtherscan(String address) {
        String lastError = "Unknown Etherscan error";
        for (String key : orderedApiKeys()) {
            AttemptResult attempt = attemptFetch(address, key);
            if (attempt.type == AttemptType.SUCCESS) {
                return attempt.records;
            }
            if (attempt.type == AttemptType.NO_TRANSACTIONS) {
                return List.of();
            }
            lastError = attempt.error;
        }

        if (syntheticFallbackEnabled) {
            return buildSyntheticHistory(address);
        }
        throw new IllegalStateException("Unable to fetch Etherscan history: " + lastError);
    }

    private List<String> orderedApiKeys() {
        if (apiKeys.isEmpty()) {
            return List.of("");
        }
        int start = Math.floorMod(keyIndex.getAndIncrement(), apiKeys.size());
        List<String> ordered = new ArrayList<>(apiKeys.size());
        for (int i = 0; i < apiKeys.size(); i++) {
            ordered.add(apiKeys.get((start + i) % apiKeys.size()));
        }
        return ordered;
    }

    @SuppressWarnings("unchecked")
    private AttemptResult attemptFetch(String address, String key) {
        try {
            String url = "https://api.etherscan.io/v2/api"
                    + "?chainid=" + etherscanChainId
                    + "&module=account&action=txlist"
                    + "&address=" + address
                    + "&startblock=0&endblock=99999999"
                    + "&page=1&offset=100&sort=desc"
                    + "&apikey=" + key;

            Map<String, Object> resp = restTemplate.getForObject(url, Map.class);
            if (resp == null) {
                return AttemptResult.retry("Empty Etherscan response");
            }

            String status = str(resp.get("status"));
            Object resultObj = resp.get("result");
            String message = str(resp.get("message"));
            String resultText = resultObj instanceof String ? (String) resultObj : "";

            if ("1".equals(status) && resultObj instanceof List<?> rawList) {
                List<Map<String, Object>> rawTxs = (List<Map<String, Object>>) rawList;
                return AttemptResult.success(parseTransactions(rawTxs));
            }

            String normalized = (message + " " + resultText).toLowerCase(Locale.ROOT);
            if (normalized.contains("no transactions found")) {
                return AttemptResult.noTransactions();
            }

            if (isRetryable(normalized)) {
                return AttemptResult.retry("Etherscan retryable response: " + message + " " + resultText);
            }

            return AttemptResult.retry("Etherscan non-success response: " + message + " " + resultText);
        } catch (Exception ex) {
            return AttemptResult.retry("Etherscan request failed: " + ex.getMessage());
        }
    }

    private boolean isRetryable(String message) {
        return message.contains("rate limit")
                || message.contains("max rate limit")
                || message.contains("invalid api key")
                || message.contains("too many invalid api key")
                || message.contains("temporarily unavailable")
                || message.contains("timeout");
    }

    private List<TxRecord> parseTransactions(List<Map<String, Object>> rawTxs) {
        List<TxRecord> records = new ArrayList<>(rawTxs.size());
        for (Map<String, Object> raw : rawTxs) {
            TxRecord rec = new TxRecord();
            rec.setHash(str(raw.get("hash")));
            rec.setBlockNumber(parseLong(raw.get("blockNumber")));
            rec.setTimestamp(parseLong(raw.get("timeStamp")));
            rec.setFrom(str(raw.get("from")));
            rec.setTo(str(raw.get("to")));

            String weiStr = str(raw.get("value"));
            try {
                BigDecimal wei = new BigDecimal(weiStr);
                rec.setValueEth(wei.multiply(BigDecimal.valueOf(WEI_TO_ETH)).doubleValue());
            } catch (Exception ex) {
                rec.setValueEth(0.0);
            }

            String contractAddress = str(raw.get("contractAddress"));
            String input = str(raw.get("input"));
            boolean hasContractAddress = !contractAddress.isBlank() && !"0x".equalsIgnoreCase(contractAddress);
            boolean hasMethodInput = !input.isBlank() && !"0x".equals(input);
            boolean isContract = hasContractAddress || hasMethodInput;
            rec.setContract(isContract);

            String methodId = input.length() >= 10 ? input.substring(0, 10).toLowerCase(Locale.ROOT) : "0x";
            rec.setMethodId(methodId);

            String toAddr = rec.getTo() != null ? rec.getTo().toLowerCase(Locale.ROOT) : "";
            rec.setProtocol(PROTOCOL_MAP.getOrDefault(toAddr, isContract ? "Contract Interaction" : "ETH Transfer"));
            rec.setRiskFlag(classifyRisk(methodId, toAddr, rec.getValueEth()));
            records.add(rec);
        }
        return records;
    }

    private List<TxRecord> buildSyntheticHistory(String address) {
        int hash = Math.abs(address.toLowerCase(Locale.ROOT).hashCode());
        Random rng = new Random(hash);
        int n = 20 + (hash % 80);
        long nowTs = Instant.now().getEpochSecond();
        long startTs = nowTs - (long) (60 + (hash % 700)) * 86400;

        List<String> protocols = List.of("Uniswap V2", "Uniswap V3", "Aave", "Compound", "SushiSwap", "1inch", "ETH Transfer");
        List<TxRecord> records = new ArrayList<>();
        long currentTs = startTs;

        for (int i = 0; i < n; i++) {
            currentTs += (long) (rng.nextInt(3) + 1) * 86400;
            if (currentTs > nowTs) {
                break;
            }

            String protocol = protocols.get(rng.nextInt(protocols.size()));
            double value = 0.001 + rng.nextDouble() * 2.0;
            String riskFlag = "NORMAL";
            int flagChance = rng.nextInt(20);
            if (flagChance == 0) {
                riskFlag = "FLASH_LOAN";
            } else if (flagChance == 1) {
                riskFlag = "LIQUIDATION";
            } else if (flagChance < 4) {
                riskFlag = "NFT";
            } else if (flagChance == 4) {
                riskFlag = "RUGPULL";
            }

            TxRecord rec = new TxRecord(
                    String.format("0x%064x", (long) hash * i),
                    15000000L + i * 100,
                    currentTs,
                    address,
                    "0x" + String.format("%040x", rng.nextLong() & 0xFFFFFFFFFFFFFFFFL),
                    value,
                    !protocol.equals("ETH Transfer"),
                    "0x" + String.format("%08x", rng.nextInt()),
                    protocol,
                    riskFlag
            );
            records.add(rec);
        }
        return records;
    }

    private String classifyRisk(String methodId, String toAddr, double valueEth) {
        if (FLASH_LOAN_SIGNATURES.contains(methodId)) {
            return "FLASH_LOAN";
        }
        if (NFT_CONTRACTS.contains(toAddr)) {
            return "NFT";
        }
        // NOTE: Small-value transactions (< 0.00001 ETH) are NOT classified as RUGPULL.
        // Many legitimate txs (ERC-20 approvals, small swaps, gas-only calls) have near-zero
        // ETH value. Labeling them RUGPULL inflated rugpullExposureScore for normal wallets.
        return "NORMAL";
    }

    private int estimateCrossChainCount(List<TxRecord> txs) {
        long[] timestamps = txs.stream().mapToLong(TxRecord::getTimestamp).sorted().toArray();
        int count = 0;
        for (int i = 1; i < timestamps.length; i++) {
            if ((timestamps[i] - timestamps[i - 1]) > 14L * 86400) {
                count++;
            }
        }
        return Math.min(count, 10);
    }

    private double computeMaxDormantDays(List<TxRecord> txs) {
        long[] timestamps = txs.stream().mapToLong(TxRecord::getTimestamp).sorted().toArray();
        double maxGap = 0;
        for (int i = 1; i < timestamps.length; i++) {
            double gap = (timestamps[i] - timestamps[i - 1]) / 86400.0;
            if (gap > maxGap) {
                maxGap = gap;
            }
        }
        return maxGap;
    }

    private static String str(Object o) {
        return o != null ? o.toString() : "";
    }

    private static long parseLong(Object o) {
        try {
            return Long.parseLong(str(o));
        } catch (Exception ex) {
            return 0L;
        }
    }

    private enum AttemptType {
        SUCCESS,
        NO_TRANSACTIONS,
        RETRY
    }

    private static final class AttemptResult {
        private final AttemptType type;
        private final List<TxRecord> records;
        private final String error;

        private AttemptResult(AttemptType type, List<TxRecord> records, String error) {
            this.type = type;
            this.records = records;
            this.error = error;
        }

        private static AttemptResult success(List<TxRecord> records) {
            return new AttemptResult(AttemptType.SUCCESS, records, "");
        }

        private static AttemptResult noTransactions() {
            return new AttemptResult(AttemptType.NO_TRANSACTIONS, Collections.emptyList(), "");
        }

        private static AttemptResult retry(String error) {
            return new AttemptResult(AttemptType.RETRY, Collections.emptyList(), error);
        }
    }
}