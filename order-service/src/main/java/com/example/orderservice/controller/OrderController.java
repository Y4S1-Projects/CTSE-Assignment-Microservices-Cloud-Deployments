package com.example.orderservice.controller;

import com.example.orderservice.dto.CreateOrderRequest;
import com.example.orderservice.dto.OrderResponse;
import com.example.orderservice.service.OrderService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

/**
 * OrderController - Placeholder implementation
 * Details will be implemented during feature development
 */
@RestController
@Tag(name = "Orders", description = "Order management endpoints")
public class OrderController {

    @Autowired
    private OrderService orderService;

    private static boolean isAdmin(String rolesHeader) {
        if (rolesHeader == null) return false;
        String upper = rolesHeader.toUpperCase();
        return upper.contains("ADMIN") || upper.contains("ROLE_ADMIN");
    }

    /** POST /orders */
    @PostMapping("")
    @Operation(summary = "Create order", description = "Create a new order (requires authentication)")
    @ApiResponse(responseCode = "201", description = "Order created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    public ResponseEntity<OrderResponse> createOrder(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @Valid @RequestBody CreateOrderRequest request) {
        String effectiveUserId = (userId == null || userId.isBlank()) ? "guest" : userId;
        return ResponseEntity.status(201).body(orderService.createOrder(effectiveUserId, request));
    }

    /** GET /orders/{id} */
    @GetMapping("/{id}")
    @Operation(summary = "Get order by ID", description = "Retrieve a specific order (requires authentication)")
    @ApiResponse(responseCode = "200", description = "Order found")
    @ApiResponse(responseCode = "404", description = "Order not found")
    public ResponseEntity<OrderResponse> getOrderById(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Roles", required = false) String roles) {
        OrderResponse order = orderService.getOrderById(id);
        boolean admin = isAdmin(roles);
        if (!admin && userId != null && !userId.isBlank() && order.getUserId() != null && !userId.equals(order.getUserId())) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(order);
    }

    /** GET /orders/my */
    @GetMapping("/my")
    @Operation(summary = "Get my orders", description = "Retrieve all orders for the current user (requires authentication)")
    @ApiResponse(responseCode = "200", description = "List of orders returned")
    public ResponseEntity<List<OrderResponse>> getMyOrders(
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        String effectiveUserId = (userId == null || userId.isBlank()) ? "guest" : userId;
        return ResponseEntity.ok(orderService.getOrdersByUserId(effectiveUserId));
    }

    /** PATCH /orders/{id}/status?status=PAID */
    @PatchMapping("/{id}/status")
    @Operation(summary = "Update order status", description = "Update the status of an order (ADMIN)")
    @ApiResponse(responseCode = "200", description = "Status updated")
    @ApiResponse(responseCode = "404", description = "Order not found")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Roles", required = false) String roles,
            @RequestParam String status) {
        // If request came through the gateway with roles, enforce ADMIN.
        // If roles header is missing (e.g., internal call from payment-service), allow it.
        if (roles != null && !roles.isBlank() && !isAdmin(roles)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(orderService.updateOrderStatus(id, status));
    }
}
