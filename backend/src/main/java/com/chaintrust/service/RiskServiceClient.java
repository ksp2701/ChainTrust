package com.chaintrust.service;

import com.chaintrust.model.RiskResult;
import com.chaintrust.model.WalletFeatures;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Map;

@Service
public class RiskServiceClient {

    private static final Logger log = LoggerFactory.getLogger(RiskServiceClient.class);

    /** Max number of predict attempts (1 initial + 2 retries). */
    private static final int MAX_ATTEMPTS = 3;

    /**
     * How long to sleep between retries when the ML service returns 502.
     * Render free tier typically wakes up within 10-20 s of the first request.
     */
    private static final long RETRY_SLEEP_MS = 8_000;

    private final RestTemplate restTemplate;
    private final FeatureService featureService;
    private final String mlServiceUrl;

    public RiskServiceClient(
        RestTemplateBuilder restTemplateBuilder,
        FeatureService featureService,
        @Value("${ml.service.url}") String mlServiceUrl
    ) {
        this.restTemplate = restTemplateBuilder
            .setConnectTimeout(Duration.ofSeconds(20))
            .setReadTimeout(Duration.ofSeconds(60))  // covers cold start + inference
            .build();
        this.featureService = featureService;
        this.mlServiceUrl = mlServiceUrl;
    }

    public RiskResult predict(WalletFeatures features) {
        WalletFeatures clean = featureService.sanitize(features);
        Map<String, Object> payload = featureService.toMlPayload(clean);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        Exception lastException = null;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                ResponseEntity<RiskResult> response = restTemplate.postForEntity(
                    mlServiceUrl + "/predict",
                    request,
                    RiskResult.class
                );

                RiskResult result = response.getBody();
                if (!response.getStatusCode().is2xxSuccessful() || result == null) {
                    return RiskResult.highRisk("ML_NON_200");
                }
                return result.normalized();

            } catch (HttpServerErrorException e) {
                // 502 Bad Gateway = Render free tier cold start — retry after a pause
                if (e.getStatusCode() == HttpStatus.BAD_GATEWAY) {
                    log.warn("ML service returned 502 (cold start), attempt {}/{} — waiting {}ms before retry",
                            attempt, MAX_ATTEMPTS, RETRY_SLEEP_MS);
                    lastException = e;
                    if (attempt < MAX_ATTEMPTS) {
                        try { Thread.sleep(RETRY_SLEEP_MS); } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            return RiskResult.highRisk("ML_UNAVAILABLE:Interrupted");
                        }
                    }
                } else {
                    // Non-502 server error — no point retrying
                    log.error("ML service returned HTTP {}: {}", e.getStatusCode(), e.getMessage());
                    return RiskResult.highRisk("ML_UNAVAILABLE:" + e.getStatusCode());
                }

            } catch (Exception e) {
                log.error("ML service call failed on attempt {}/{}: {}", attempt, MAX_ATTEMPTS, e.getMessage());
                lastException = e;
                if (attempt < MAX_ATTEMPTS) {
                    try { Thread.sleep(RETRY_SLEEP_MS); } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return RiskResult.highRisk("ML_UNAVAILABLE:Interrupted");
                    }
                }
            }
        }

        String reason = lastException != null
            ? "ML_UNAVAILABLE:" + lastException.getClass().getSimpleName()
            : "ML_UNAVAILABLE";
        log.error("ML service unreachable after {} attempts: {}", MAX_ATTEMPTS, reason);
        return RiskResult.highRisk(reason);
    }
}
