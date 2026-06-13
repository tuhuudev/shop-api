package com.learn.shopapi.dto;

import com.learn.shopapi.entity.InventoryMovement;
import java.time.LocalDateTime;

/** Mot dong nhat ky bien dong ton kho tra ve cho client. */
public record InventoryMovementResponse(
        Long id,
        Long productId,
        int changeQty,
        String reason,
        Long orderId,
        LocalDateTime createdAt,
        String createdBy
) {
    public static InventoryMovementResponse from(InventoryMovement m) {
        return new InventoryMovementResponse(
                m.getId(),
                m.getProductId(),
                m.getChangeQty(),
                m.getReason().name(),
                m.getOrderId(),
                m.getCreatedAt(),
                m.getCreatedBy());
    }
}
