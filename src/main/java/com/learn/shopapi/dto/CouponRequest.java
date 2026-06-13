package com.learn.shopapi.dto;

import com.learn.shopapi.entity.CouponType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Tao/cap nhat coupon (ADMIN). */
public record CouponRequest(
        @NotBlank @Size(max = 64) String code,
        @NotNull CouponType type,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal value,
        BigDecimal minOrderAmount,
        Integer maxUses,
        LocalDateTime validFrom,
        LocalDateTime validTo,
        Boolean active
) { }
