package com.example.paymentservice.repository;

import com.example.paymentservice.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, String> {
    Optional<Payment> findByOrderId(String orderId);
    List<Payment> findByStatus(String status);
    List<Payment> findByUserId(String userId);
    List<Payment> findByItemId(String itemId);
    List<Payment> findByIsSuccessCheckout(Boolean isSuccessCheckout);
    List<Payment> findAllByOrderByCreatedAtDesc();
}
