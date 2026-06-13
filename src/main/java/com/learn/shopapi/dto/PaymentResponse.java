package com.learn.shopapi.dto;

import com.learn.shopapi.entity.Payment;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Ban ghi thanh toan tra ve. */
public record PaymentResponse(
        Long id,
        Long orderId,
        String provider,
        String providerRef,
        BigDecimal amount,
        String status,
        LocalDateTime createdAt
) {
    public static PaymentResponse from(Payment p) {
        return new PaymentResponse(
                p.getId(), p.getOrderId(), p.getProvider(), p.getProviderRef(),
                p.getAmount(), p.getStatus().name(), p.getCreatedAt());
    }
}
