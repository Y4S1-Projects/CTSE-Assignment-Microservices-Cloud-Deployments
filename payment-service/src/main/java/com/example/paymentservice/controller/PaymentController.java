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

    /** POST /payments/checkout */
    @PostMapping("/checkout")
    @Operation(summary = "Process checkout and update catalog stock")
    @ApiResponse(responseCode = "200", description = "Checkout successful")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    public ResponseEntity<PaymentResponse> checkout(@RequestBody PaymentRequest request) {
        return ResponseEntity.ok(paymentService.checkout(request));
    }

    /** GET /payments/orders */
    @GetMapping("/orders")
    @Operation(summary = "List all orders / payments")
    public ResponseEntity<List<PaymentResponse>> getAllOrders() {
        return ResponseEntity.ok(paymentService.getAllOrders());
    }

    /** GET /payments/{id} */
    @GetMapping("/{id}")
    @Operation(summary = "Get payment by id")
    @ApiResponse(responseCode = "200", description = "Found")
    @ApiResponse(responseCode = "404", description = "Not found")
    public ResponseEntity<PaymentResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(paymentService.getPaymentById(id));
    }

    /** GET /payments/order/{orderId} */
    @GetMapping("/order/{orderId}")
    @Operation(summary = "Get payment by orderId")
    public ResponseEntity<PaymentResponse> getByOrderId(@PathVariable String orderId) {
        return ResponseEntity.ok(paymentService.getPaymentByOrderId(orderId));
    }

    /** GET /payments/user/{userId} */
    @GetMapping("/user/{userId}")
    @Operation(summary = "Get all payments for a user")
    public ResponseEntity<List<PaymentResponse>> getByUser(@PathVariable String userId) {
        return ResponseEntity.ok(paymentService.getPaymentsByUser(userId));
    }
}
