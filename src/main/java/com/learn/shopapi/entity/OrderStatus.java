package com.learn.shopapi.entity;

import java.util.EnumSet;
import java.util.Set;

/**
 * Trang thai cua mot don hang + LUAT CHUYEN TRANG THAI (state machine).
 *
 * Khong cho nhay lung tung (vi du DELIVERED ve PENDING, hay huy don da giao).
 * Luong hop le:
 *   PENDING -> PAID hoac CANCELLED
 *   PAID    -> SHIPPED hoac CANCELLED
 *   SHIPPED -> (ket thuc)
 *   CANCELLED -> (ket thuc)
 */
public enum OrderStatus {
    PENDING,    // moi tao, chua thanh toan
    PAID,       // da thanh toan
    SHIPPED,    // da giao van chuyen
    CANCELLED;  // da huy

    /** Cac trang thai duoc phep chuyen TIEP tu trang thai hien tai. */
    public Set<OrderStatus> allowedNext() {
        return switch (this) {
            case PENDING -> EnumSet.of(PAID, CANCELLED);
            case PAID -> EnumSet.of(SHIPPED, CANCELLED);
            case SHIPPED, CANCELLED -> EnumSet.noneOf(OrderStatus.class);
        };
    }

    public boolean canTransitionTo(OrderStatus next) {
        return allowedNext().contains(next);
    }
}
