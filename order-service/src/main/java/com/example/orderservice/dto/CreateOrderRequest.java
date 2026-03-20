package com.example.orderservice.dto;

import lombok.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {
    @NotEmpty(message = "items cannot be empty")
    @Valid
    private List<OrderItemRequest> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemRequest {
        /**
         * Can be either the catalog DB id (UUID) OR the business itemId (e.g. ITEM-0001).
         * Order-service will try both when validating against catalog-service.
         */
        @NotBlank(message = "itemId is required")
        private String itemId;

        @NotNull(message = "quantity is required")
        @Min(value = 1, message = "quantity must be >= 1")
        private Integer quantity;
    }
}
