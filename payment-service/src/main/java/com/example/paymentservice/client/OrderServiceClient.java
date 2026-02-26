package com.example.paymentservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Feign client for Order Service
 * Used for inter-service communication: Payment -> Order
 * Implementation will be added during feature development
 */
@FeignClient(name = "order-service", url = "${service.order.url:http://order-service:8083}")
public interface OrderServiceClient {
    
    @PatchMapping("/orders/{id}/status")
    Object updateOrderStatus(
            @PathVariable("id") String orderId,
            @RequestParam String status);
}
