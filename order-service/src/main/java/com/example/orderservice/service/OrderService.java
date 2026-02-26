package com.example.orderservice.service;

import com.example.orderservice.dto.CreateOrderRequest;
import com.example.orderservice.dto.OrderResponse;
import java.util.List;

/**
 * OrderService interface
 * Implementation will be added during feature development
 */
public interface OrderService {
    OrderResponse createOrder(String userId, CreateOrderRequest request);
    OrderResponse getOrderById(String orderId);
    List<OrderResponse> getOrdersByUserId(String userId);
    OrderResponse updateOrderStatus(String orderId, String status);
}
