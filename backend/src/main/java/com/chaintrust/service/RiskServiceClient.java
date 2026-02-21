package com.chaintrust.service;

import com.chaintrust.model.RiskResult;
import com.chaintrust.model.WalletFeatures;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Map;

@Service
public class RiskServiceClient {

    private static final Logger log = LoggerFactory.getLogger(RiskServiceClient.class);

    private final RestTemplate restTemplate;
    private final FeatureService featureService;
    private final String mlServiceUrl;

    public RiskServiceClient(
        RestTemplateBuilder restTemplateBuilder,
        FeatureService featureService,
        @Value("${ml.service.url}") String mlServiceUrl,
        @Value("${ml.service.connect-timeout-ms:15000}") long connectTimeoutMs,
        @Value("${ml.service.read-timeout-ms:30000}") long readTimeoutMs
    ) {
        long safeConnectMs = Math.max(3000L, connectTimeoutMs);
        long safeReadMs = Math.max(5000L, readTimeoutMs);
        this.restTemplate = restTemplateBuilder
            .setConnectTimeout(Duration.ofMillis(safeConnectMs))
            .setReadTimeout(Duration.ofMillis(safeReadMs))
            .build();
        this.featureService = featureService;
        this.mlServiceUrl = mlServiceUrl;
    }

    public RiskResult predict(WalletFeatures features) {
        WalletFeatures clean = featureService.sanitize(features);
        Map<String, Object> payload = featureService.toMlPayload(clean);

        Exception lastError = null;
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                RiskResult result = callMlPredict(payload);
                if (result != null) {
                    return result.normalized();
                }
                return RiskResult.highRisk("ML_NON_200");
            } catch (Exception e) {
                lastError = e;
                log.warn("ML predict attempt {} failed: {}", attempt, e.toString());
                if (attempt == 1) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        String reason = "ML_UNAVAILABLE";
        if (lastError != null) {
            reason = "ML_UNAVAILABLE:" + lastError.getClass().getSimpleName();
        }
        return RiskResult.highRisk(reason);
    }

    private RiskResult callMlPredict(Map<String, Object> payload) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            ResponseEntity<RiskResult> response = restTemplate.postForEntity(
                mlServiceUrl + "/predict",
                request,
                RiskResult.class
            );

            RiskResult result = response.getBody();
            if (!response.getStatusCode().is2xxSuccessful() || result == null) {
                return null;
            }
            return result;
        } catch (Exception e) {
            throw e;
        }
    }
}
