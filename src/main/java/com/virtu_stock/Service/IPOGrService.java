package com.virtu_stock.Service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.virtu_stock.DTO.Response.IPOGrResponseDTO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IPOGrService {
    @Value("${ipogr.api.key}")
    private String apiKey;
    @Value("${ipogr.url}")
    private String ipoGrURL;

    private final RestTemplate restTemplate;

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-api-key", apiKey);
        headers.set("Content-Type", "application/json");
        return headers;
    }

    public IPOGrResponseDTO getIPOs(String status, String type, Integer months, Integer limit) {
        Map<String, String> params = new HashMap<>();
        if (status != null)
            params.put("status", status);
        if (type != null)
            params.put("type", type);
        if (months != null) {
            params.put("months", months.toString());
        }
        if (limit != null) {
            params.put("months", months.toString());
        }

        StringBuilder url = new StringBuilder(ipoGrURL + "/ipos?");
        params.forEach((key, value) -> url.append(key).append("=").append(value).append("&"));

        HttpEntity<Void> entity = new HttpEntity<>(createHeaders());
        ResponseEntity<IPOGrResponseDTO> response = restTemplate.exchange(
                url.toString(),
                HttpMethod.GET,
                entity,
                IPOGrResponseDTO.class);
        System.out.println(response);
        return response.getBody();
    }

}
