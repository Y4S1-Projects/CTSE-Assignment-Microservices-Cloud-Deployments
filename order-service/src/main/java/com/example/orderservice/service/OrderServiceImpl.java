package com.example.orderservice.service;

import com.example.orderservice.client.CatalogServiceClient;
import com.example.orderservice.dto.CreateOrderRequest;
import com.example.orderservice.dto.OrderResponse;
import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.OrderItem;
import com.example.orderservice.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * OrderServiceImpl - Placeholder implementation
 * Full implementation will be added during feature development
 */
@Service
public class OrderServiceImpl implements OrderService {
    private static final Logger logger = LoggerFactory.getLogger(OrderServiceImpl.class);

    @Autowired(required = false)
    private OrderRepository orderRepository;

    @Autowired(required = false)
    private CatalogServiceClient catalogServiceClient;

    private static BigDecimal asBigDecimal(Object value) {
        if (value == null) return null;
        if (value instanceof BigDecimal bd) return bd;
        if (value instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    private static Integer asInt(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean isAvailable(Map<String, Object> item) {
        Object available = item.get("available");
        if (available instanceof Boolean b) return b;
        if (available instanceof String s) return Boolean.parseBoolean(s);
        return true;
    }

    private Map<String, Object> resolveCatalogItem(String maybeIdOrItemId) {
        if (catalogServiceClient == null) return null;
        Map<String, Object> byId = catalogServiceClient.getItemById(maybeIdOrItemId);
        if (byId != null) return byId;
        return catalogServiceClient.getItemByItemId(maybeIdOrItemId);
    }

    private OrderResponse toResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .items(order.getItems() == null ? List.of() : order.getItems().stream()
                        .map(i -> OrderResponse.OrderItemResponse.builder()
                                .id(i.getId())
                                .catalogItemId(i.getCatalogItemId())
                                .itemId(i.getItemId())
                                .itemName(i.getItemName())
                                .unitPrice(i.getUnitPrice())
                                .quantity(i.getQuantity())
                                .lineTotal(i.getLineTotal())
                                .build())
                        .collect(Collectors.toList()))
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional
    public OrderResponse createOrder(String userId, CreateOrderRequest request) {
        logger.info("Creating order for user: {}", userId);
        if (orderRepository == null) {
            throw new IllegalStateException("OrderRepository is not available");
        }
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one item");
        }

        Order order = Order.builder()
                .userId(userId)
                .status("CREATED")
                .items(new ArrayList<>())
                .build();

        BigDecimal total = BigDecimal.ZERO;

        for (CreateOrderRequest.OrderItemRequest itemReq : request.getItems()) {
            if (itemReq == null) continue;
            String incomingItemId = itemReq.getItemId();
            Integer qty = itemReq.getQuantity();
            if (incomingItemId == null || incomingItemId.isBlank()) {
                throw new IllegalArgumentException("Item ID is required");
            }
            if (qty == null || qty < 1) {
                throw new IllegalArgumentException("Quantity must be >= 1");
            }

            Map<String, Object> catalogItem = resolveCatalogItem(incomingItemId);
            if (catalogItem == null) {
                throw new IllegalArgumentException(
                        "Catalog item not found or catalog unreachable for id: " + incomingItemId
                                + ". Ensure catalog-service is running and CATALOG_SERVICE_URL matches your environment (e.g. http://localhost:8082 locally).");
            }

            if (!isAvailable(catalogItem)) {
                throw new IllegalArgumentException("Catalog item is not available: " + incomingItemId);
            }

            Integer stock = asInt(catalogItem.get("stockCount"));
            if (stock != null && stock < qty) {
                throw new IllegalArgumentException("Insufficient stock for item: " + incomingItemId);
            }

            BigDecimal unitPrice = asBigDecimal(catalogItem.get("price"));
            if (unitPrice == null) {
                throw new IllegalArgumentException("Catalog item price missing for: " + incomingItemId);
            }

            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(qty));
            total = total.add(lineTotal);

            OrderItem oi = OrderItem.builder()
                    .order(order)
                    .catalogItemId((String) catalogItem.get("id"))
                    .itemId((String) catalogItem.get("itemId"))
                    .itemName((String) catalogItem.get("name"))
                    .unitPrice(unitPrice)
                    .quantity(qty)
                    .lineTotal(lineTotal)
                    .build();
            order.getItems().add(oi);
        }

        order.setTotalAmount(total);
        Order saved = orderRepository.save(order);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(String orderId) {
        logger.info("Fetching order with ID: {}", orderId);
        if (orderRepository == null) {
            throw new IllegalStateException("OrderRepository is not available");
        }
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        return toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByUserId(String userId) {
        logger.info("Fetching orders for user: {}", userId);
        if (orderRepository == null) {
            throw new IllegalStateException("OrderRepository is not available");
        }
        return orderRepository.findByUserId(userId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(String orderId, String status) {
        logger.info("Updating order {} status to {}", orderId, status);
        if (orderRepository == null) {
            throw new IllegalStateException("OrderRepository is not available");
        }
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("status is required");
        }
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        order.setStatus(status.trim().toUpperCase());
        Order saved = orderRepository.save(order);
        return toResponse(saved);
    }
}
