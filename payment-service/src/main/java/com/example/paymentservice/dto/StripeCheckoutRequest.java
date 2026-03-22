package com.example.paymentservice.dto;

import lombok.*;
import java.math.BigDecimal;

/**
 * Request DTO for initiating a Stripe PaymentIntent.
 *
 * POST /payments/stripe/create-intent
 *
 * Amount must be in the smallest currency unit:
 *   USD $25.00  → amount: 2500  (cents)
 *   LKR 2500.00 → amount: 250000 (cents)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StripeCheckoutRequest {
    private String orderId;       // your internal order ID e.g. "ORD-20260322-001"
    private String userId;        // user placing the order
    private String itemId;        // catalog item ID (optional, for enrichment)
    private Integer quantity;
    private Long amount;          // in smallest currency unit (cents/paisa)
    private String currency;      // "usd" or "lkr" — lowercase, Stripe requirement
    private String description;   // shown on Stripe dashboard e.g. "2x Chicken Kottu"
}