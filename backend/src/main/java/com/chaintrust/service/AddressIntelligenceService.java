package com.chaintrust.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetCode;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class AddressIntelligenceService {

    private final Web3j web3j;
    private final RestTemplate restTemplate;
    private final List<String> etherscanApiKeys;
    private final AtomicInteger keyIndex = new AtomicInteger(0);
    private final long etherscanChainId;
    private final Set<String> burnAddresses;
    private final Set<String> knownProtocolContracts;
    private final boolean rejectContractAddresses;
    private final boolean requireContractCheckSuccess;

    public AddressIntelligenceService(
            Web3j web3j,
            RestTemplateBuilder restTemplateBuilder,
            @Value("${etherscan.api-keys:}") String etherscanApiKeysCsv,
            @Value("${etherscan.chain-id:1}") long etherscanChainId,
            @Value("${loan.policy.burn-addresses:0x0000000000000000000000000000000000000000,0x000000000000000000000000000000000000dEaD}") String burnAddressesCsv,
            @Value("${loan.policy.known-contract-addresses:0x7a250d5630b4cf539739df2c5dacb4c659f2488d,0xe592427a0aece92de3edee1f18e0157c05861564,0x68b3465833fb72a70ecdf485e0e4c7bd8665fc45,0x1111111254fb6c44bac0bed2854e76f90643097d}") String knownContractsCsv,
            @Value("${loan.policy.reject-contract-addresses:true}") boolean rejectContractAddresses,
            @Value("${loan.policy.require-contract-check-success:false}") boolean requireContractCheckSuccess) {
        this.web3j = web3j;
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(10))
                .build();
        this.etherscanApiKeys = Arrays.stream(etherscanApiKeysCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
        this.etherscanChainId = etherscanChainId;
        this.burnAddresses = parseAddressSet(burnAddressesCsv);
        this.knownProtocolContracts = parseAddressSet(knownContractsCsv);
        this.rejectContractAddresses = rejectContractAddresses;
        this.requireContractCheckSuccess = requireContractCheckSuccess;
    }

    public AddressAssessment assess(String address) {
        String lower = normalize(address);
        boolean isBurn = burnAddresses.contains(lower);
        boolean isKnownProtocol = knownProtocolContracts.contains(lower);

        boolean contractCheckSucceeded = false;
        boolean isSmartContract = false;
        String contractCheckError = null;

        try {
            EthGetCode codeResp = web3j.ethGetCode(address, DefaultBlockParameterName.LATEST).send();
            String code = codeResp != null ? codeResp.getCode() : null;
            isSmartContract = isContractCode(code);
            contractCheckSucceeded = true;
        } catch (Exception rpcError) {
            contractCheckError = rpcError.getMessage();
            try {
                String etherscanCode = fetchCodeFromEtherscan(address);
                if (etherscanCode != null) {
                    isSmartContract = isContractCode(etherscanCode);
                    contractCheckSucceeded = true;
                    contractCheckError = null;
                }
            } catch (Exception etherscanError) {
                contractCheckError = "RPC: " + safeMessage(rpcError) + " | Etherscan: " + safeMessage(etherscanError);
            }

            if (!contractCheckSucceeded && requireContractCheckSuccess && rejectContractAddresses) {
                throw new IllegalStateException("Unable to verify address type: " + contractCheckError, rpcError);
            }
        }

        return new AddressAssessment(
                isBurn,
                isKnownProtocol,
                isSmartContract,
                contractCheckSucceeded,
                contractCheckError
        );
    }

    private String fetchCodeFromEtherscan(String address) {
        if (etherscanApiKeys.isEmpty()) {
            return null;
        }

        List<String> keys = orderedApiKeys();
        for (String key : keys) {
            String url = "https://api.etherscan.io/v2/api"
                    + "?chainid=" + etherscanChainId
                    + "&module=proxy&action=eth_getCode"
                    + "&address=" + address
                    + "&tag=latest"
                    + "&apikey=" + key;

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response == null) {
                continue;
            }

            Object resultObj = response.get("result");
            if (resultObj != null) {
                return resultObj.toString();
            }
        }
        return null;
    }

    private List<String> orderedApiKeys() {
        int start = Math.floorMod(keyIndex.getAndIncrement(), etherscanApiKeys.size());
        String[] ordered = new String[etherscanApiKeys.size()];
        for (int i = 0; i < etherscanApiKeys.size(); i++) {
            ordered[i] = etherscanApiKeys.get((start + i) % etherscanApiKeys.size());
        }
        return List.of(ordered);
    }

    private static boolean isContractCode(String code) {
        if (code == null) {
            return false;
        }
        String c = code.trim();
        return !c.isBlank() && !"0x".equalsIgnoreCase(c) && !"0x0".equalsIgnoreCase(c);
    }

    private static Set<String> parseAddressSet(String csv) {
        Set<String> set = new HashSet<>();
        Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(AddressIntelligenceService::normalize)
                .forEach(set::add);
        return set;
    }

    private static String normalize(String address) {
        return address == null ? "" : address.trim().toLowerCase(Locale.ROOT);
    }

    private static String safeMessage(Throwable ex) {
        return ex != null && ex.getMessage() != null ? ex.getMessage() : "unknown";
    }

    public record AddressAssessment(
            boolean burnAddress,
            boolean knownProtocolContract,
            boolean smartContract,
            boolean contractCheckSucceeded,
            String contractCheckError
    ) {}
}