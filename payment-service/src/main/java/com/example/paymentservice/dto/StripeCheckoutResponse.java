package com.example.paymentservice.dto;

import lombok.*;

/**
 * Response DTO returned after creating a Stripe PaymentIntent.
 *
 * The frontend uses clientSecret to confirm the payment directly
 * with Stripe using Stripe.js — your backend never sees card details.
 *
 * Frontend usage (Stripe.js):
 *   const stripe = Stripe("pk_test_...");
 *   const result = await stripe.confirmCardPayment(data.clientSecret, {
 *     payment_method: { card: cardElement }
 *   });
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StripeCheckoutResponse {
    private String paymentIntentId;  // pi_xxx — use this to query status later
    private String clientSecret;     // send to frontend to confirm payment
    private Long amount;             // in smallest currency unit
    private String currency;
    private String status;           // "requires_payment_method" initially
    private String orderId;
    private String paymentRecordId;  // your internal Payment entity ID
}