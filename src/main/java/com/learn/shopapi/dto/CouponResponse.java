package com.learn.shopapi.dto;

import com.learn.shopapi.entity.Coupon;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Coupon tra ve cho client. */
public record CouponResponse(
        Long id,
        String code,
        String type,
        BigDecimal value,
        BigDecimal minOrderAmount,
        Integer maxUses,
        int usedCount,
        LocalDateTime validFrom,
        LocalDateTime validTo,
        boolean active
) {
    public static CouponResponse from(Coupon c) {
        return new CouponResponse(
                c.getId(), c.getCode(), c.getType().name(), c.getValue(),
                c.getMinOrderAmount(), c.getMaxUses(), c.getUsedCount(),
                c.getValidFrom(), c.getValidTo(), c.isActive());
    }
}
