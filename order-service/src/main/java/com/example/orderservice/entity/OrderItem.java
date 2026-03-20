package com.example.orderservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Order order;

    /** Catalog DB id (UUID) if available */
    @Column(length = 100)
    private String catalogItemId;

    /** Business itemId (e.g. ITEM-0001) if available */
    @Column(length = 100)
    private String itemId;

    @Column(length = 255)
    private String itemName;

    private BigDecimal unitPrice;

    @Column(nullable = false)
    private Integer quantity;

    private BigDecimal lineTotal;
}

