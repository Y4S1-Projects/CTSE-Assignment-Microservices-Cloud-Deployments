package com.example.paymentservice.service;

import com.example.paymentservice.dto.PaymentRequest;
import com.example.paymentservice.dto.PaymentResponse;
import java.util.List;

public interface PaymentService {
    PaymentResponse checkout(PaymentRequest request);
    List<PaymentResponse> getAllOrders();
    PaymentResponse getPaymentById(String paymentId);
    PaymentResponse getPaymentByOrderId(String orderId);
    List<PaymentResponse> getPaymentsByUser(String userId);
}
