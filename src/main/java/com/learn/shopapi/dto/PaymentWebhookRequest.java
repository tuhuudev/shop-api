package com.learn.shopapi.dto;

import com.learn.shopapi.entity.PaymentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Payload webhook tu cong thanh toan (mock).
 * Chu ky HMAC duoc tinh tren chuoi chuan: providerRef|orderId|amount|status (xem PaymentService).
 */
public record PaymentWebhookRequest(
        @NotBlank String providerRef,
        @NotNull Long orderId,
        @NotNull BigDecimal amount,
        @NotNull PaymentStatus status
) { }
