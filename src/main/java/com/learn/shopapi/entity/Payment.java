package com.learn.shopapi.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Ban ghi thanh toan ung voi 1 lan callback tu cong thanh toan.
 * providerRef: ma giao dich ben cong (DUY NHAT) -> dung lam khoa idempotency cho webhook
 * (cong goi lai cung providerRef -> khong xu ly 2 lan).
 */
@Entity
@Table(name = "payments", indexes = @Index(name = "idx_payments_order", columnList = "order_id"))
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(nullable = false, length = 32)
    private String provider;

    @Column(name = "provider_ref", nullable = false, unique = true, length = 128)
    private String providerRef;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PaymentStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Payment() { }

    public Payment(Long orderId, String provider, String providerRef, BigDecimal amount, PaymentStatus status) {
        this.orderId = orderId;
        this.provider = provider;
        this.providerRef = providerRef;
        this.amount = amount;
        this.status = status;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getOrderId() { return orderId; }
    public String getProvider() { return provider; }
    public String getProviderRef() { return providerRef; }
    public BigDecimal getAmount() { return amount; }
    public PaymentStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
