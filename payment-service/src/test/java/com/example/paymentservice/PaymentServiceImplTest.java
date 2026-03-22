package com.example.paymentservice;

import com.example.paymentservice.client.CatalogServiceClient;
import com.example.paymentservice.client.OrderServiceClient;
import com.example.paymentservice.dto.PaymentRequest;
import com.example.paymentservice.dto.PaymentResponse;
import com.example.paymentservice.entity.Payment;
import com.example.paymentservice.repository.PaymentRepository;
import com.example.paymentservice.service.PaymentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private CatalogServiceClient catalogServiceClient;

    @Mock
    private OrderServiceClient orderServiceClient;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private Payment samplePayment;

    @BeforeEach
    void setUp() {
        samplePayment = Payment.builder()
                .id("pay-001")
                .itemId("ITEM-0001")
                .orderId("order-001")
                .userId("user-001")
                .quantity(2)
                .amount(new BigDecimal("25.00"))
                .paymentMethod("CARD")
                .status("COMPLETED")
                .isSuccessCheckout(true)
                .reference("PAY-ABCD1234")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ── Test 1: Successful checkout ───────────────────────────────────────────
    @Test
    void checkout_shouldSavePaymentAndReturnResponse() {
        PaymentRequest request = PaymentRequest.builder()
                .itemId("ITEM-0001")
                .orderId("order-001")
                .userId("user-001")
                .quantity(2)
                .amount(new BigDecimal("25.00"))
                .paymentMethod("CARD")
                .build();

        when(paymentRepository.save(any(Payment.class))).thenReturn(samplePayment);

        PaymentResponse response = paymentService.checkout(request);

        assertNotNull(response);
        assertEquals("COMPLETED", response.getStatus());
        assertTrue(response.getIsSuccessCheckout());
        assertNotNull(response.getReference());
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    // ── Test 2: Get payment by ID - found ─────────────────────────────────────
    @Test
    void getPaymentById_shouldReturnPayment_whenFound() {
        when(paymentRepository.findById("pay-001")).thenReturn(Optional.of(samplePayment));

        PaymentResponse response = paymentService.getPaymentById("pay-001");

        assertNotNull(response);
        assertEquals("pay-001", response.getId());
        assertEquals("ITEM-0001", response.getItemId());
    }

    // ── Test 3: Get payment by ID - not found ─────────────────────────────────
    @Test
    void getPaymentById_shouldThrowException_whenNotFound() {
        when(paymentRepository.findById("invalid-id")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> paymentService.getPaymentById("invalid-id"));

        assertTrue(ex.getMessage().contains("Payment not found"));
    }

    // ── Test 4: Get payments by user ──────────────────────────────────────────
    @Test
    void getPaymentsByUser_shouldReturnList() {
        when(paymentRepository.findByUserId("user-001")).thenReturn(List.of(samplePayment));

        List<PaymentResponse> result = paymentService.getPaymentsByUser("user-001");

        assertEquals(1, result.size());
        assertEquals("user-001", result.get(0).getUserId());
    }

    // ── Test 5: Get payment by order ID - not found ───────────────────────────
    @Test
    void getPaymentByOrderId_shouldThrowException_whenNotFound() {
        when(paymentRepository.findByOrderId("bad-order")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> paymentService.getPaymentByOrderId("bad-order"));

        assertTrue(ex.getMessage().contains("Payment not found for order"));
    }

    // ── Test 6: Checkout triggers order status update ─────────────────────────
    @Test
    void checkout_shouldCallOrderService_whenOrderIdPresent() {
        PaymentRequest request = PaymentRequest.builder()
                .itemId("ITEM-0001")
                .orderId("order-999")
                .userId("user-001")
                .quantity(1)
                .amount(new BigDecimal("12.00"))
                .paymentMethod("ONLINE")
                .build();

        when(paymentRepository.save(any(Payment.class))).thenReturn(samplePayment);

        paymentService.checkout(request);

        verify(orderServiceClient, times(1)).updateOrderStatus("order-001", "PAID");
    }
}