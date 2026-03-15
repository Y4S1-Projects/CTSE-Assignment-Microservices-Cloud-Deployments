package com.example.orderservice.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * HTTP client for Catalog Service using RestTemplate
 * (Replaced Spring Cloud OpenFeign for Spring Boot 4 compatibility)
 */
@Component
public class CatalogServiceClient {

    private final RestTemplate restTemplate;
    private final String catalogServiceUrl;

    public CatalogServiceClient(
            RestTemplate restTemplate,
            @Value("${service.catalog.url:http://catalog-service:8082}") String catalogServiceUrl) {
        this.restTemplate = restTemplate;
        this.catalogServiceUrl = catalogServiceUrl;
    }

    public Object getItem(String itemId) {
        try {
            return restTemplate.getForObject(
                catalogServiceUrl + "/catalog/items/" + itemId, Object.class);
        } catch (Exception e) {
            return null;
        }
    }
}
