package com.example.paymentservice.service;

import com.example.paymentservice.dto.PaymentRequest;
import com.example.paymentservice.dto.PaymentResponse;

/**
 * PaymentService interface
 * Implementation will be added during feature development
 */
public interface PaymentService {
    PaymentResponse processPayment(PaymentRequest request);
    PaymentResponse getPaymentByOrderId(String orderId);
    PaymentResponse getPaymentById(String paymentId);
}
