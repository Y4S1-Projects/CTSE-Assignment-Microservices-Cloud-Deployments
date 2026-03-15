package com.example.paymentservice.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private String id;
    private String itemId;
    private String orderId;
    private String userId;
    private Integer quantity;
    private BigDecimal amount;
    private String paymentMethod;
    private String status;
    private Boolean isSuccessCheckout;
    private String reference;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Enriched catalog fields (populated from catalog service lookup)
    private String itemName;
    private String itemCategory;
    private Integer remainingStock;
}
