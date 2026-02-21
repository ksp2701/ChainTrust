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

    /** How many times to attempt /predict total (1 initial + N-1 retries). */
    private static final int MAX_ATTEMPTS = 4;

    /** Wait between retries when we get 502 (Render cold start). */
    private static final long RETRY_SLEEP_MS = 10_000;

    /**
     * After detecting 502, we ping /health in a loop waiting for the service
     * to wake up before retrying /predict. Max wait = WARMUP_POLLS * WARMUP_POLL_MS.
     */
    private static final int    WARMUP_POLLS    = 8;
    private static final long   WARMUP_POLL_MS  = 5_000;  // 8 × 5s = up to 40s warmup

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
            .setReadTimeout(Duration.ofSeconds(60))
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
                if (e.getStatusCode() == HttpStatus.BAD_GATEWAY) {
                    log.warn("ML service 502 on attempt {}/{} — waiting for cold-start warmup", attempt, MAX_ATTEMPTS);
                    if (attempt < MAX_ATTEMPTS) {
                        // Poll /health until the service is up, then retry /predict
                        boolean warmedUp = waitForHealth();
                        if (!warmedUp) {
                            log.warn("ML service did not respond to health checks — retrying /predict anyway");
                        }
                    }
                } else {
                    log.error("ML service HTTP {}: {}", e.getStatusCode(), e.getMessage());
                    return RiskResult.highRisk("ML_UNAVAILABLE:" + e.getStatusCode());
                }

            } catch (Exception e) {
                log.error("ML service call failed attempt {}/{}: {}", attempt, MAX_ATTEMPTS, e.getMessage());
                if (attempt < MAX_ATTEMPTS) {
                    sleep(RETRY_SLEEP_MS);
                }
            }
        }

        log.error("ML service unreachable after {} attempts", MAX_ATTEMPTS);
        return RiskResult.highRisk("ML_UNAVAILABLE:MaxRetriesExceeded");
    }

    /**
     * Polls GET /health until the ML service responds with 200 or we time out.
     * Returns true if the service came up within the poll window.
     */
    private boolean waitForHealth() {
        String healthUrl = mlServiceUrl + "/health";
        for (int i = 0; i < WARMUP_POLLS; i++) {
            sleep(WARMUP_POLL_MS);
            try {
                ResponseEntity<String> resp = restTemplate.getForEntity(healthUrl, String.class);
                if (resp.getStatusCode().is2xxSuccessful()) {
                    log.info("ML service is up after {}s warmup — retrying /predict", (i + 1) * (WARMUP_POLL_MS / 1000));
                    return true;
                }
            } catch (Exception ignored) {
                // Still waking up — keep polling
            }
        }
        return false;
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
