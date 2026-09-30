package com.virtu_stock.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;

import java.util.HashMap;
import java.util.Map;

@Service
public class IPOAlertsService {

    @Value("${ipoalerts.api.key}")
    private String apiKey;
    @Value("${ipoalerts.url}")
    private String ipoAlertsURL;

    private final RestTemplate restTemplate;

    public IPOAlertsService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-api-key", apiKey);
        headers.set("Content-Type", "application/json");
        return headers;
    }

    @Cacheable(value = "ipos", key = "#status + '_' + #type + '_' + #page + '_' + #limit")
    public Map<String, Object> getIPOs(String status, String type, int page, int limit) {
        Map<String, String> params = new HashMap<>();
        if (status != null)
            params.put("status", status);
        if (type != null)
            params.put("type", type);
        params.put("page", String.valueOf(page));
        params.put("limit", String.valueOf(limit));

        StringBuilder url = new StringBuilder(ipoAlertsURL + "/ipos?");
        params.forEach((key, value) -> url.append(key).append("=").append(value).append("&"));

        HttpEntity<Void> entity = new HttpEntity<>(createHeaders());
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url.toString(),
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<Map<String, Object>>() {
                });
        return response.getBody();
    }

    @Cacheable(value = "ipo", key = "#identifier")
    public Map<String, Object> getIPO(String identifier) {
        String url = ipoAlertsURL + "/ipos/" + identifier;
        HttpEntity<String> entity = new HttpEntity<>(createHeaders());
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url.toString(),
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<Map<String, Object>>() {
                });

        return response.getBody();
    }
}