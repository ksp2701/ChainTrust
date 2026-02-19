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

import java.time.Duration;
import java.util.Map;

@Service
public class RiskServiceClient {

    private final RestTemplate restTemplate;
    private final FeatureService featureService;
    private final String mlServiceUrl;

    public RiskServiceClient(
        RestTemplateBuilder restTemplateBuilder,
        FeatureService featureService,
        @Value("${ml.service.url}") String mlServiceUrl
    ) {
        this.restTemplate = restTemplateBuilder
            .setConnectTimeout(Duration.ofSeconds(3))
            .setReadTimeout(Duration.ofSeconds(5))
            .build();
        this.featureService = featureService;
        this.mlServiceUrl = mlServiceUrl;
    }

    public RiskResult predict(WalletFeatures features) {
        try {
            WalletFeatures clean = featureService.sanitize(features);
            Map<String, Object> payload = featureService.toMlPayload(clean);

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
                return RiskResult.highRisk("ML_NON_200");
            }
            return result.normalized();
        } catch (Exception e) {
            return RiskResult.highRisk("ML_UNAVAILABLE");
        }
    }
}
