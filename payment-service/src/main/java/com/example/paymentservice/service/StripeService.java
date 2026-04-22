package com.example.paymentservice.service;

import com.example.paymentservice.dto.StripeCheckoutRequest;
import com.example.paymentservice.dto.StripeCheckoutResponse;
import com.example.paymentservice.entity.Payment;
import com.example.paymentservice.repository.PaymentRepository;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class StripeService {

    private static final Logger logger = LoggerFactory.getLogger(StripeService.class);

    @Value("${stripe.secret.key}")
    private String secretKey;

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    private final PaymentRepository paymentRepository;

    public StripeService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    // Set the Stripe API key once on startup
    @PostConstruct
    public void init() {
        Stripe.apiKey = secretKey;
        logger.info("Stripe initialized in sandbox mode");
    }

    // ── 1. Create PaymentIntent ───────────────────────────────────────────────

    /**
     * Creates a Stripe PaymentIntent and saves a PENDING payment record.
     * Returns the clientSecret to the frontend for payment confirmation.
     */
    public StripeCheckoutResponse createPaymentIntent(StripeCheckoutRequest request) throws StripeException {
        logger.info("Creating Stripe PaymentIntent for order {} amount {} {}",
                request.getOrderId(), request.getAmount(), request.getCurrency());

        // Build the PaymentIntent with Stripe
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(request.getAmount())
                .setCurrency(request.getCurrency() != null ? request.getCurrency() : "usd")
                .setDescription(request.getDescription() != null
                        ? request.getDescription()
                        : "Order " + request.getOrderId())
                // Store your internal IDs in Stripe metadata — visible in dashboard
                .putMetadata("orderId", request.getOrderId() != null ? request.getOrderId() : "")
                .putMetadata("userId", request.getUserId() != null ? request.getUserId() : "guest")
                .putMetadata("itemId", request.getItemId() != null ? request.getItemId() : "")
                // Automatically confirm with card — frontend must provide payment_method
                .addPaymentMethodType("card")
                .build();

        PaymentIntent intent = PaymentIntent.create(params);
        logger.info("PaymentIntent created: {}", intent.getId());

        // Save a PENDING record in our DB immediately
        Payment payment = Payment.builder()
                .orderId(request.getOrderId())
                .userId(request.getUserId() != null ? request.getUserId() : "guest")
                .itemId(request.getItemId() != null ? request.getItemId() : "unknown")
                .quantity(request.getQuantity() != null ? request.getQuantity() : 1)
                .amount(BigDecimal.valueOf(request.getAmount()))
                .paymentMethod("STRIPE")
                .status("PENDING")
                .isSuccessCheckout(false)
                .reference(intent.getId())   // store pi_xxx as our reference
                .build();

        Payment saved = paymentRepository.save(payment);
        logger.info("Payment record {} saved with Stripe ref {}", saved.getId(), intent.getId());

        return StripeCheckoutResponse.builder()
                .paymentIntentId(intent.getId())
                .clientSecret(intent.getClientSecret())
                .amount(intent.getAmount())
                .currency(intent.getCurrency())
                .status(intent.getStatus())
                .orderId(request.getOrderId())
                .paymentRecordId(saved.getId())
                .build();
    }

    // ── 2. Handle Webhook ─────────────────────────────────────────────────────

    /**
     * Verifies the Stripe webhook signature and processes the event.
     * Called by StripeController when Stripe POSTs to /payments/stripe/webhook.
     *
     * Key events handled:
     *   payment_intent.succeeded  → mark payment COMPLETED
     *   payment_intent.payment_failed → mark payment FAILED
     */
    public void handleWebhook(String payload, String sigHeader) {
        Event event;

        // Step 1: Verify the webhook came from Stripe (not a random POST)
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            logger.warn("Stripe webhook signature verification failed: {}", e.getMessage());
            throw new RuntimeException("Invalid Stripe webhook signature");
        }

        logger.info("Stripe webhook received: {} {}", event.getType(), event.getId());

        // Step 2: Parse the raw event payload JSON directly using Gson
        // This avoids all SDK version mismatch issues entirely
        com.google.gson.JsonObject fullEvent = com.google.gson.JsonParser
                .parseString(payload).getAsJsonObject();
        com.google.gson.JsonObject dataObj = fullEvent
                .getAsJsonObject("data")
                .getAsJsonObject("object");

        // Step 3: Handle event types using raw JSON fields
        switch (event.getType()) {

            case "payment_intent.succeeded" -> {
                String piId = dataObj.has("id") ? dataObj.get("id").getAsString() : null;
                logger.info("PaymentIntent succeeded: {}", piId);
                if (piId != null) updatePaymentStatus(piId, "COMPLETED", true);
            }

            case "charge.succeeded" -> {
                String piId = dataObj.has("payment_intent")
                        && !dataObj.get("payment_intent").isJsonNull()
                        ? dataObj.get("payment_intent").getAsString() : null;
                logger.info("Charge succeeded for PaymentIntent: {}", piId);
                if (piId != null) updatePaymentStatus(piId, "COMPLETED", true);
            }

            case "charge.failed" -> {
                String piId = dataObj.has("payment_intent")
                        && !dataObj.get("payment_intent").isJsonNull()
                        ? dataObj.get("payment_intent").getAsString() : null;
                logger.warn("Charge failed for PaymentIntent: {}", piId);
                if (piId != null) updatePaymentStatus(piId, "FAILED", false);
            }

            case "payment_intent.payment_failed" -> {
                String piId = dataObj.has("id") ? dataObj.get("id").getAsString() : null;
                logger.warn("PaymentIntent failed: {}", piId);
                if (piId != null) updatePaymentStatus(piId, "FAILED", false);
            }

            case "payment_intent.canceled" -> {
                String piId = dataObj.has("id") ? dataObj.get("id").getAsString() : null;
                logger.info("PaymentIntent canceled: {}", piId);
                if (piId != null) updatePaymentStatus(piId, "CANCELLED", false);
            }

            default -> logger.debug("Unhandled Stripe event type: {}", event.getType());
        }
    }

    // ── 3. Get PaymentIntent status ───────────────────────────────────────────

    /**
     * Fetches the current status of a PaymentIntent directly from Stripe.
     * Useful as a fallback if the webhook was missed.
     */
    public PaymentIntent getPaymentIntentStatus(String paymentIntentId) throws StripeException {
        return PaymentIntent.retrieve(paymentIntentId);
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private void updatePaymentStatus(String stripeRef, String status, boolean success) {
        paymentRepository.findByReference(stripeRef).ifPresentOrElse(payment -> {
            payment.setStatus(status);
            payment.setIsSuccessCheckout(success);
            paymentRepository.save(payment);
            logger.info("Payment {} updated to {}", payment.getId(), status);
        }, () -> logger.warn("No payment record found for Stripe ref {}", stripeRef));
    }
}