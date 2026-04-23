package com.example.paymentservice.controller;

import com.example.paymentservice.dto.PaymentRequest;
import com.example.paymentservice.dto.PaymentResponse;
import com.example.paymentservice.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;

/**
 * NOTE: context-path is /payments (application.properties).
 * Controller paths below are relative to /payments.
 * External URL: /payments/checkout → controller sees /checkout
 */
@RestController
@Tag(name = "Payments", description = "Checkout & payment processing endpoints")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    private static boolean hasRole(String rolesHeader, String role) {
        if (rolesHeader == null || rolesHeader.isBlank()) return false;
        String expected = role.toUpperCase(Locale.ROOT);
        for (String token : rolesHeader.split("[,;\\s]+")) {
            String normalized = token.trim().toUpperCase(Locale.ROOT);
            if (normalized.isEmpty()) {
                continue;
            }
            if (normalized.equals(expected) || normalized.equals("ROLE_" + expected)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isAdmin(String rolesHeader) {
        return hasRole(rolesHeader, "ADMIN");
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /** POST /payments/checkout */
    @PostMapping("/checkout")
    @Operation(summary = "Process checkout and update catalog stock")
    @ApiResponse(responseCode = "200", description = "Checkout successful")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    public ResponseEntity<PaymentResponse> checkout(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestBody PaymentRequest request) {
        if (isBlank(userId)) {
            return ResponseEntity.status(401).build();
        }
        request.setUserId(userId.trim());
        return ResponseEntity.ok(paymentService.checkout(request));
    }

    /** GET /payments/orders */
    @GetMapping("/orders")
    @Operation(summary = "List all orders / payments")
    public ResponseEntity<List<PaymentResponse>> getAllOrders(
            @RequestHeader(value = "X-User-Roles", required = false) String roles) {
        if (!isAdmin(roles)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(paymentService.getAllOrders());
    }

    /** GET /payments/{id} */
    @GetMapping("/{id}")
    @Operation(summary = "Get payment by id")
    @ApiResponse(responseCode = "200", description = "Found")
    @ApiResponse(responseCode = "404", description = "Not found")
    public ResponseEntity<PaymentResponse> getById(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Roles", required = false) String roles) {
        PaymentResponse payment = paymentService.getPaymentById(id);
        boolean admin = isAdmin(roles);
        String requesterUserId = isBlank(userId) ? null : userId.trim();
        if (!admin) {
            if (requesterUserId == null) {
                return ResponseEntity.status(401).build();
            }
            if (!requesterUserId.equals(payment.getUserId())) {
                return ResponseEntity.status(403).build();
            }
        }
        return ResponseEntity.ok(payment);
    }

    /** GET /payments/order/{orderId} */
    @GetMapping("/order/{orderId}")
    @Operation(summary = "Get payment by orderId")
    public ResponseEntity<PaymentResponse> getByOrderId(
            @PathVariable String orderId,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Roles", required = false) String roles) {
        PaymentResponse payment = paymentService.getPaymentByOrderId(orderId);
        boolean admin = isAdmin(roles);
        String requesterUserId = isBlank(userId) ? null : userId.trim();
        if (!admin) {
            if (requesterUserId == null) {
                return ResponseEntity.status(401).build();
            }
            if (!requesterUserId.equals(payment.getUserId())) {
                return ResponseEntity.status(403).build();
            }
        }
        return ResponseEntity.ok(payment);
    }

    /** GET /payments/user/{userId} */
    @GetMapping("/user/{userId}")
    @Operation(summary = "Get all payments for a user")
    public ResponseEntity<List<PaymentResponse>> getByUser(
            @PathVariable String userId,
            @RequestHeader(value = "X-User-Id", required = false) String requesterUserId,
            @RequestHeader(value = "X-User-Roles", required = false) String roles) {
        boolean admin = isAdmin(roles);
        String effectiveRequesterUserId = isBlank(requesterUserId) ? null : requesterUserId.trim();
        if (!admin) {
            if (effectiveRequesterUserId == null) {
                return ResponseEntity.status(401).build();
            }
            if (!effectiveRequesterUserId.equals(userId)) {
                return ResponseEntity.status(403).build();
            }
        }
        return ResponseEntity.ok(paymentService.getPaymentsByUser(userId));
    }
}
