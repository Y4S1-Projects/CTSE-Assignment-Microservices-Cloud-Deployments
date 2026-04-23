package com.example.paymentservice.client;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestClientException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * HTTP client for Order Service using RestTemplate
 * (Replaced Spring Cloud OpenFeign for Spring Boot 4 compatibility)
 */
@Component
public class OrderServiceClient {
    private static final String SERVICE_ROLE_HEADER = "X-Service-Role";
    private static final String SERVICE_PAYMENT_ROLE = "SERVICE_PAYMENT";

    private final RestTemplate restTemplate;
    private final String orderServiceUrl;

    public OrderServiceClient(
            RestTemplate restTemplate,
            @Value("${service.order.url:http://order-service:8083}") String orderServiceUrl) {
        this.restTemplate = restTemplate;
        this.orderServiceUrl = orderServiceUrl;
    }

    public Object updateOrderStatus(String orderId, String status) {
        try {
            String url = UriComponentsBuilder
                .fromUriString(orderServiceUrl + "/orders/{id}/status")
                .queryParam("status", status)
                .buildAndExpand(orderId)
                .toUriString();
            HttpHeaders headers = new HttpHeaders();
            headers.add(SERVICE_ROLE_HEADER, SERVICE_PAYMENT_ROLE);
            return restTemplate.exchange(url, HttpMethod.PATCH, new HttpEntity<>(headers), Object.class).getBody();
        } catch (RestClientException | IllegalArgumentException e) {
            return null;
        }
    }
}
