package com.example.orderservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign client for Catalog Service
 * Used for inter-service communication: Order -> Catalog
 * Implementation will be added during feature development
 */
@FeignClient(name = "catalog-service", url = "${service.catalog.url:http://catalog-service:8082}")
public interface CatalogServiceClient {
    
    @GetMapping("/catalog/items/{id}")
    Object getItem(@PathVariable("id") String itemId);
}
