package com.example.paymentservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /** Business-level item ID from catalog service (e.g. "ITEM-1234") */
    @Column(nullable = false)
    private String itemId;

    /** Optional link to an order record */
    private String orderId;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String paymentMethod;   // CARD, CASH, ONLINE

    @Column(length = 50)
    private String status;          // PENDING, COMPLETED, FAILED

    /** True once checkout is confirmed and stock has been decremented */
    @Column
    private Boolean isSuccessCheckout;

    private String reference;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null)              status = "PENDING";
        if (isSuccessCheckout == null)   isSuccessCheckout = false;
        if (paymentMethod == null)       paymentMethod = "CARD";
        if (quantity == null)            quantity = 1;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
