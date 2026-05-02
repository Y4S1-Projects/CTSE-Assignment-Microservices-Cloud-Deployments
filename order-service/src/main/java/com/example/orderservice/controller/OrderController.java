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
import java.util.Locale;


@RestController
@Tag(name = "Orders", description = "Order management endpoints")
public class OrderController {

    @Autowired
    private OrderService orderService;

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

    private static boolean isServicePayment(String rolesHeader) {
        return hasRole(rolesHeader, "SERVICE_PAYMENT");
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /** POST /orders */
    @PostMapping({ "", "/" })
    @Operation(summary = "Create order", description = "Create a new order (requires authentication)")
    @ApiResponse(responseCode = "201", description = "Order created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    public ResponseEntity<OrderResponse> createOrder(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @Valid @RequestBody CreateOrderRequest request) {
        if (isBlank(userId)) {
            return ResponseEntity.status(401).build();
        }
        String effectiveUserId = userId.trim();
        return ResponseEntity.status(201).body(orderService.createOrder(effectiveUserId, request));
    }

    /** GET /orders/{id} */
    @GetMapping({ "/{id}", "/{id}/" })
    @Operation(summary = "Get order by ID", description = "Retrieve a specific order (requires authentication)")
    @ApiResponse(responseCode = "200", description = "Order found")
    @ApiResponse(responseCode = "404", description = "Order not found")
    public ResponseEntity<OrderResponse> getOrderById(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Roles", required = false) String roles) {
        OrderResponse order = orderService.getOrderById(id);
        boolean admin = isAdmin(roles);
        String requesterUserId = isBlank(userId) ? null : userId.trim();
        if (!admin) {
            if (requesterUserId == null) {
                return ResponseEntity.status(401).build();
            }
            if (!requesterUserId.equals(order.getUserId())) {
                return ResponseEntity.status(403).build();
            }
        }
        return ResponseEntity.ok(order);
    }

    //Test comment

    /** GET /orders/my */
    @GetMapping({ "/my", "/my/" })
    @Operation(summary = "Get my orders", description = "Retrieve all orders for the current user (requires authentication)")
    @ApiResponse(responseCode = "200", description = "List of orders returned")
    public ResponseEntity<List<OrderResponse>> getMyOrders(
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        if (isBlank(userId)) {
            return ResponseEntity.status(401).build();
        }
        String effectiveUserId = userId.trim();
        return ResponseEntity.ok(orderService.getOrdersByUserId(effectiveUserId));
    }

    /** PATCH /orders/{id}/status?status=PAID */
    @PatchMapping({ "/{id}/status", "/{id}/status/" })
    @Operation(summary = "Update order status", description = "Update the status of an order (ADMIN)")
    @ApiResponse(responseCode = "200", description = "Status updated")
    @ApiResponse(responseCode = "404", description = "Order not found")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Roles", required = false) String roles,
            @RequestHeader(value = "X-Service-Role", required = false) String serviceRole,
            @RequestParam String status) {
        if (!isAdmin(roles) && !isServicePayment(serviceRole)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(orderService.updateOrderStatus(id, status));
    }
}
