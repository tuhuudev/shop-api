package com.learn.shopapi.dto;

import com.learn.shopapi.entity.Cart;

import java.math.BigDecimal;
import java.util.List;

/** Gio hang tra ve cho client: cac dong + tong tien tam tinh (theo gia hien tai). */
public record CartResponse(List<Line> items, BigDecimal totalAmount) {

    public record Line(Long productId, String productName, int quantity,
                       BigDecimal unitPrice, BigDecimal lineTotal) { }

    public static CartResponse from(Cart cart) {
        List<Line> lines = cart.getItems().stream()
                .map(i -> {
                    BigDecimal unit = i.getProduct().getPrice();
                    BigDecimal line = unit.multiply(BigDecimal.valueOf(i.getQuantity()));
                    return new Line(i.getProduct().getId(), i.getProduct().getName(),
                            i.getQuantity(), unit, line);
                })
                .toList();
        BigDecimal total = lines.stream().map(Line::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartResponse(lines, total);
    }
}
