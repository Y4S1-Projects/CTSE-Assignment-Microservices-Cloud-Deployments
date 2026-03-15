package com.example.paymentservice.service;

import com.example.paymentservice.client.CatalogServiceClient;
import com.example.paymentservice.client.OrderServiceClient;
import com.example.paymentservice.dto.PaymentRequest;
import com.example.paymentservice.dto.PaymentResponse;
import com.example.paymentservice.entity.Payment;
import com.example.paymentservice.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PaymentServiceImpl implements PaymentService {
    private static final Logger logger = LoggerFactory.getLogger(PaymentServiceImpl.class);

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired(required = false)
    private CatalogServiceClient catalogServiceClient;

    @Autowired(required = false)
    private OrderServiceClient orderServiceClient;

    // ── helper ────────────────────────────────────────────────────────────────

    private PaymentResponse toResponse(Payment p) {
        return PaymentResponse.builder()
                .id(p.getId())
                .itemId(p.getItemId())
                .orderId(p.getOrderId())
                .userId(p.getUserId())
                .quantity(p.getQuantity())
                .amount(p.getAmount())
                .paymentMethod(p.getPaymentMethod())
                .status(p.getStatus())
                .isSuccessCheckout(p.getIsSuccessCheckout())
                .reference(p.getReference())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }

    private PaymentResponse enrichWithCatalog(PaymentResponse resp) {
        if (catalogServiceClient != null && resp.getItemId() != null) {
            try {
                Map<String, Object> item = catalogServiceClient.getItemByItemId(resp.getItemId());
                if (item != null) {
                    resp.setItemName((String) item.get("name"));
                    resp.setItemCategory((String) item.get("category"));
                    Object stock = item.get("stockCount");
                    if (stock instanceof Number) resp.setRemainingStock(((Number) stock).intValue());
                }
            } catch (Exception e) {
                logger.warn("Could not enrich payment {} with catalog info: {}", resp.getId(), e.getMessage());
            }
        }
        return resp;
    }

    // ── checkout ──────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public PaymentResponse checkout(PaymentRequest request) {
        logger.info("Processing checkout for item {} by user {}", request.getItemId(), request.getUserId());

        // Simulate payment processing (always succeeds in this demo)
        String reference = "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Payment payment = Payment.builder()
                .itemId(request.getItemId())
                .orderId(request.getOrderId())
                .userId(request.getUserId() != null ? request.getUserId() : "guest")
                .quantity(request.getQuantity() != null ? request.getQuantity() : 1)
                .amount(request.getAmount() != null ? request.getAmount() : BigDecimal.ZERO)
                .paymentMethod(request.getPaymentMethod() != null ? request.getPaymentMethod() : "CARD")
                .status("COMPLETED")
                .isSuccessCheckout(true)
                .reference(reference)
                .build();

        Payment saved = paymentRepository.save(payment);
        logger.info("Payment {} saved with reference {}", saved.getId(), reference);

        // Decrement catalog stock in real-time
        if (catalogServiceClient != null && saved.getIsSuccessCheckout()) {
            Map<String, Object> updatedItem = catalogServiceClient.decrementStock(
                    saved.getItemId(), saved.getQuantity());
            if (updatedItem != null) {
                Object remaining = updatedItem.get("stockCount");
                logger.info("Stock decremented. Remaining for {}: {}", saved.getItemId(), remaining);
            }
        }

        return enrichWithCatalog(toResponse(saved));
    }

    // ── queries ───────────────────────────────────────────────────────────────

    @Override
    public List<PaymentResponse> getAllOrders() {
        return paymentRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(p -> enrichWithCatalog(toResponse(p)))
                .collect(Collectors.toList());
    }

    @Override
    public PaymentResponse getPaymentById(String paymentId) {
        Payment p = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));
        return enrichWithCatalog(toResponse(p));
    }

    @Override
    public PaymentResponse getPaymentByOrderId(String orderId) {
        Payment p = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Payment not found for order: " + orderId));
        return enrichWithCatalog(toResponse(p));
    }

    @Override
    public List<PaymentResponse> getPaymentsByUser(String userId) {
        return paymentRepository.findByUserId(userId)
                .stream()
                .map(p -> enrichWithCatalog(toResponse(p)))
                .collect(Collectors.toList());
    }
}
