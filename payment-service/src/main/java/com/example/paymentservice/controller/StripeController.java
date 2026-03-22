package com.example.paymentservice.controller;

import com.example.paymentservice.dto.StripeCheckoutRequest;
import com.example.paymentservice.dto.StripeCheckoutResponse;
import com.example.paymentservice.service.StripeService;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Stripe Payment Gateway Controller
 *
 * Endpoints (context-path /payments already applied):
 *   POST /payments/stripe/create-intent  — create PaymentIntent, return clientSecret
 *   POST /payments/stripe/webhook        — receive Stripe server-to-server events
 *   GET  /payments/stripe/status/{id}    — check PaymentIntent status
 *
 * Demo test card:
 *   Number: 4242 4242 4242 4242
 *   Expiry: any future date
 *   CVC:    any 3 digits
 *   ZIP:    any 5 digits
 */
@RestController
@RequestMapping("/stripe")
@Tag(name = "Stripe", description = "Stripe payment gateway integration (sandbox)")
public class StripeController {

    private static final Logger logger = LoggerFactory.getLogger(StripeController.class);

    private final StripeService stripeService;

    public StripeController(StripeService stripeService) {
        this.stripeService = stripeService;
    }

    // ── 1. Create PaymentIntent ───────────────────────────────────────────────

    /**
     * Creates a Stripe PaymentIntent and returns the clientSecret.
     * The frontend uses the clientSecret with Stripe.js to collect card details
     * and confirm the payment — your server never touches card data.
     *
     * Example request:
     * {
     *   "orderId": "ORD-20260322-001",
     *   "userId": "user123",
     *   "itemId": "ITEM-456",
     *   "quantity": 2,
     *   "amount": 250000,
     *   "currency": "usd",
     *   "description": "2x Chicken Kottu"
     * }
     */
    @PostMapping("/create-intent")
    @Operation(
        summary = "Create Stripe PaymentIntent",
        description = "Creates a PaymentIntent and returns clientSecret for frontend payment confirmation. " +
                      "Test card: 4242 4242 4242 4242 / any future date / any CVC"
    )
    public ResponseEntity<?> createPaymentIntent(@RequestBody StripeCheckoutRequest request) {
        try {
            StripeCheckoutResponse response = stripeService.createPaymentIntent(request);
            return ResponseEntity.ok(response);
        } catch (StripeException e) {
            logger.error("Stripe error creating PaymentIntent: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of(
                        "error", "Stripe error",
                        "message", e.getMessage(),
                        "stripeCode", e.getCode() != null ? e.getCode() : "unknown"
                    ));
        }
    }

    // ── 2. Webhook ────────────────────────────────────────────────────────────

    /**
     * Receives Stripe webhook events (server-to-server).
     * Stripe signs every request with a Stripe-Signature header.
     * We verify the signature before processing.
     *
     * To test locally:
     *   1. Install Stripe CLI
     *   2. Run: stripe listen --forward-to localhost:8084/payments/stripe/webhook
     *   3. The CLI prints a whsec_... key — put that in STRIPE_WEBHOOK_SECRET
     *
     * IMPORTANT: Spring must NOT parse this body as JSON — we need the raw bytes
     * for signature verification. That's why the param is String, not a DTO.
     */
    @PostMapping("/webhook")
    @Operation(
        summary = "Stripe webhook receiver",
        description = "Called by Stripe after payment events. Verifies signature and updates payment status."
    )
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        try {
            stripeService.handleWebhook(payload, sigHeader);
            return ResponseEntity.ok("OK");
        } catch (RuntimeException e) {
            // Invalid signature — return 400 so Stripe retries
            logger.warn("Webhook rejected: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // ── 3. Check status ───────────────────────────────────────────────────────

    /**
     * Fetches the current status of a PaymentIntent directly from Stripe.
     * Use this as a fallback if you want to confirm status without waiting
     * for the webhook (e.g. after the user returns from the payment page).
     */
    @GetMapping("/status/{paymentIntentId}")
    @Operation(
        summary = "Get PaymentIntent status",
        description = "Retrieves the current status of a Stripe PaymentIntent directly from Stripe API."
    )
    public ResponseEntity<?> getStatus(@PathVariable String paymentIntentId) {
        try {
            PaymentIntent intent = stripeService.getPaymentIntentStatus(paymentIntentId);
            return ResponseEntity.ok(Map.of(
                "paymentIntentId", intent.getId(),
                "status", intent.getStatus(),
                "amount", intent.getAmount(),
                "currency", intent.getCurrency(),
                "description", intent.getDescription() != null ? intent.getDescription() : ""
            ));
        } catch (StripeException e) {
            logger.error("Stripe error fetching status: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}