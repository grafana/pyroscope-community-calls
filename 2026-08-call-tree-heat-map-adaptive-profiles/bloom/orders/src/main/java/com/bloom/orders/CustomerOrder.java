package com.bloom.orders;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "customer_orders")
public class CustomerOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "order_seq")
    @SequenceGenerator(name = "order_seq", sequenceName = "order_seq", allocationSize = 50)
    private Long id;

    @Column(nullable = false)
    private String customerId;

    private String status;

    private long subtotalCents;

    private long discountCents;

    private long totalCents;

    @Column(length = 500)
    private String promotions;

    private Instant createdAt;

    protected CustomerOrder() {
    }

    public CustomerOrder(String customerId, String status, long subtotalCents, long discountCents,
                         long totalCents, String promotions, Instant createdAt) {
        this.customerId = customerId;
        this.status = status;
        this.subtotalCents = subtotalCents;
        this.discountCents = discountCents;
        this.totalCents = totalCents;
        this.promotions = promotions;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getStatus() {
        return status;
    }

    public long getSubtotalCents() {
        return subtotalCents;
    }

    public long getDiscountCents() {
        return discountCents;
    }

    public long getTotalCents() {
        return totalCents;
    }

    public String getPromotions() {
        return promotions;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
