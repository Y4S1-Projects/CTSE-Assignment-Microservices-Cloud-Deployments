package com.example.paymentservice.client;

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
                .fromHttpUrl(orderServiceUrl + "/orders/{id}/status")
                .queryParam("status", status)
                .buildAndExpand(orderId)
                .toUriString();
            restTemplate.patchForObject(url, null, Object.class);
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
