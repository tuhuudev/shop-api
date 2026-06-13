package com.learn.shopapi.dto;

import com.learn.shopapi.entity.Order;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Du lieu don hang tra ve cho client. */
public record OrderResponse(
        Long id,
        Long customerId,
        String customerName,
        LocalDateTime orderDate,
        String status,
        BigDecimal totalAmount,
        List<Line> items
) {
    public record Line(String productName, int quantity, BigDecimal unitPrice, BigDecimal lineTotal) { }

    public static OrderResponse from(Order o) {
        List<Line> lines = o.getItems().stream()
                .map(i -> new Line(
                        i.getProduct().getName(),
                        i.getQuantity(),
                        i.getUnitPrice(),
                        i.getLineTotal()))
                .toList();
        return new OrderResponse(
                o.getId(),
                o.getCustomer().getId(),
                o.getCustomer().getName(),
                o.getOrderDate(),
                o.getStatus().name(),
                o.getTotalAmount(),
                lines
        );
    }
}
