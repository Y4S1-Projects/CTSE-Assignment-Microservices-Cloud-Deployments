package com.example.paymentservice.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {
    private String itemId;          // catalog business itemId (e.g. "ITEM-1234")
    private String userId;
    private Integer quantity;
    private BigDecimal amount;
    private String paymentMethod;   // CARD, CASH, ONLINE
    private String orderId;         // optional
}
