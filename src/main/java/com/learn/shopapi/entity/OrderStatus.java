package com.learn.shopapi.entity;

import java.util.EnumSet;
import java.util.Set;

/**
 * Trang thai cua mot don hang + LUAT CHUYEN TRANG THAI (state machine).
 *
 * Khong cho nhay lung tung (vi du DELIVERED ve PENDING, hay huy don da giao).
 * Luong hop le:
 *   PENDING -> PAID hoac CANCELLED
 *   PAID    -> SHIPPED, CANCELLED hoac REFUNDED
 *   SHIPPED -> REFUNDED
 *   CANCELLED / REFUNDED -> (ket thuc)
 */
public enum OrderStatus {
    PENDING,    // moi tao, chua thanh toan
    PAID,       // da thanh toan
    SHIPPED,    // da giao van chuyen
    CANCELLED,  // da huy (chua/khong thanh toan)
    REFUNDED;   // da hoan tien (sau khi da thanh toan)

    /** Cac trang thai duoc phep chuyen TIEP tu trang thai hien tai. */
    public Set<OrderStatus> allowedNext() {
        return switch (this) {
            case PENDING -> EnumSet.of(PAID, CANCELLED);
            case PAID -> EnumSet.of(SHIPPED, CANCELLED, REFUNDED);
            case SHIPPED -> EnumSet.of(REFUNDED);
            case CANCELLED, REFUNDED -> EnumSet.noneOf(OrderStatus.class);
        };
    }

    public boolean canTransitionTo(OrderStatus next) {
        return allowedNext().contains(next);
    }

    /** Trang thai "ket thuc da tra hang ve kho" -> can hoan ton kho. */
    public boolean isStockReturning() {
        return this == CANCELLED || this == REFUNDED;
    }
}
