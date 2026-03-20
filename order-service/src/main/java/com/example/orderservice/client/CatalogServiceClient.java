package com.example.orderservice.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/**
 * HTTP client for Catalog Service using RestTemplate
 * (Replaced Spring Cloud OpenFeign for Spring Boot 4 compatibility)
 */
@Component
public class CatalogServiceClient {

    private static final Logger log = LoggerFactory.getLogger(CatalogServiceClient.class);

    private final RestTemplate restTemplate;
    private final String catalogServiceUrl;

    public CatalogServiceClient(
            RestTemplate restTemplate,
            @Value("${service.catalog.url:http://localhost:8082}") String catalogServiceUrl) {
        this.restTemplate = restTemplate;
        this.catalogServiceUrl = catalogServiceUrl;
    }

    /** Fetch item by catalog DB id (UUID) */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getItemById(String id) {
        String url = catalogServiceUrl + "/catalog/items/" + id;
        try {
            return restTemplate.getForObject(url, Map.class);
        } catch (RestClientException e) {
            log.warn("Catalog getItemById failed for {}: {} — check service.catalog.url (use http://localhost:8082 locally, or CATALOG_SERVICE_URL in Docker)", url, e.getMessage());
            return null;
        }
    }

    /** Fetch item by business itemId (e.g. ITEM-0001) */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getItemByItemId(String itemId) {
        String url = UriComponentsBuilder
                .fromUriString(catalogServiceUrl + "/catalog/items/by-item-id/{itemId}")
                .buildAndExpand(itemId)
                .toUriString();
        try {
            return restTemplate.getForObject(url, Map.class);
        } catch (RestClientException e) {
            log.warn("Catalog getItemByItemId failed for {}: {}", url, e.getMessage());
            return null;
        }
    }
}
