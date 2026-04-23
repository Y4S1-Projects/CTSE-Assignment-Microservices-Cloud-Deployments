package com.example.paymentservice.client;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/**
 * HTTP client for Catalog Service using RestTemplate.
 * Calls /catalog/items/{itemId}/decrement-stock to update inventory after checkout.
 */
@Component
public class CatalogServiceClient {
    private static final Logger logger = LoggerFactory.getLogger(CatalogServiceClient.class);
    private static final String SERVICE_ROLE_HEADER = "X-Service-Role";
    private static final String SERVICE_PAYMENT_ROLE = "SERVICE_PAYMENT";

    private final RestTemplate restTemplate;
    private final String catalogServiceUrl;

    public CatalogServiceClient(
            RestTemplate restTemplate,
            @Value("${service.catalog.url:http://catalog-service:8082}") String catalogServiceUrl) {
        this.restTemplate = restTemplate;
        this.catalogServiceUrl = catalogServiceUrl;
    }

    /**
     * @param itemId   catalog business itemId (e.g. "ITEM-1234")
     * @param quantity number of units sold
     * @return the updated item response map, or null on failure
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> decrementStock(String itemId, int quantity) {
        try {
            String url = UriComponentsBuilder
                    .fromUriString(catalogServiceUrl + "/catalog/items/{itemId}/decrement-stock")
                    .queryParam("quantity", quantity)
                    .buildAndExpand(itemId)
                    .toUriString();

            logger.info("Decrementing stock for item {} by {}", itemId, quantity);
            HttpHeaders headers = new HttpHeaders();
            headers.add(SERVICE_ROLE_HEADER, SERVICE_PAYMENT_ROLE);
            Map<String, Object> response = restTemplate.postForObject(url, new HttpEntity<>(headers), Map.class);
            logger.info("Stock decremented successfully for item {}", itemId);
            return response;
        } catch (RestClientException | IllegalArgumentException e) {
            logger.error("Failed to decrement stock for item {}: {}", itemId, e.getMessage());
            return null;
        }
    }

    /**
     * Fetch item details from catalog service.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getItemByItemId(String itemId) {
        try {
            String url = UriComponentsBuilder
                    .fromUriString(catalogServiceUrl + "/catalog/items/by-item-id/{itemId}")
                    .buildAndExpand(itemId)
                    .toUriString();
            return restTemplate.getForObject(url, Map.class);
        } catch (RestClientException | IllegalArgumentException e) {
            logger.error("Failed to fetch item {}: {}", itemId, e.getMessage());
            return null;
        }
    }
}
