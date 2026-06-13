package com.learn.shopapi.entity;

import com.learn.shopapi.common.Auditable;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Ma giam gia.
 * - type=PERCENT: value la % (0..100), giam = subtotal * value/100.
 * - type=FIXED:   value la so tien co dinh, giam = min(value, subtotal).
 * Co the gioi han: khoang hieu luc [validFrom, validTo], so lan dung toi da (maxUses),
 * gia tri don toi thieu (minOrderAmount). usedCount dem so lan da dung (tang ATOMIC).
 */
@Entity
@Table(name = "coupons")
public class Coupon extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private CouponType type;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal value;

    @Column(name = "min_order_amount", precision = 12, scale = 2)
    private BigDecimal minOrderAmount;

    @Column(name = "max_uses")
    private Integer maxUses;

    @Column(name = "used_count", nullable = false)
    private int usedCount;

    @Column(name = "valid_from")
    private LocalDateTime validFrom;

    @Column(name = "valid_to")
    private LocalDateTime validTo;

    @Column(nullable = false)
    private boolean active = true;

    protected Coupon() { }

    public Coupon(String code, CouponType type, BigDecimal value) {
        this.code = code;
        this.type = type;
        this.value = value;
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public CouponType getType() { return type; }
    public void setType(CouponType type) { this.type = type; }
    public BigDecimal getValue() { return value; }
    public void setValue(BigDecimal value) { this.value = value; }
    public BigDecimal getMinOrderAmount() { return minOrderAmount; }
    public void setMinOrderAmount(BigDecimal minOrderAmount) { this.minOrderAmount = minOrderAmount; }
    public Integer getMaxUses() { return maxUses; }
    public void setMaxUses(Integer maxUses) { this.maxUses = maxUses; }
    public int getUsedCount() { return usedCount; }
    public LocalDateTime getValidFrom() { return validFrom; }
    public void setValidFrom(LocalDateTime validFrom) { this.validFrom = validFrom; }
    public LocalDateTime getValidTo() { return validTo; }
    public void setValidTo(LocalDateTime validTo) { this.validTo = validTo; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
