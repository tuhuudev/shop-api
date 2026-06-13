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
        BigDecimal subtotal,
        BigDecimal discountAmount,
        String couponCode,
        BigDecimal totalAmount,
        Shipping shipping,
        List<Line> items
) {
    public record Line(String productName, int quantity, BigDecimal unitPrice, BigDecimal lineTotal) { }

    public record Shipping(String recipient, String phone, String line1, String line2,
                           String city, String province, String postalCode, String country) { }

    public static OrderResponse from(Order o) {
        List<Line> lines = o.getItems().stream()
                .map(i -> new Line(
                        i.getProduct().getName(),
                        i.getQuantity(),
                        i.getUnitPrice(),
                        i.getLineTotal()))
                .toList();
        var sa = o.getShippingAddress();
        Shipping shipping = (sa == null || sa.getRecipient() == null) ? null
                : new Shipping(sa.getRecipient(), sa.getPhone(), sa.getLine1(), sa.getLine2(),
                        sa.getCity(), sa.getProvince(), sa.getPostalCode(), sa.getCountry());
        return new OrderResponse(
                o.getId(),
                o.getCustomer().getId(),
                o.getCustomer().getName(),
                o.getOrderDate(),
                o.getStatus().name(),
                o.subtotal(),
                o.getDiscountAmount(),
                o.getCouponCode(),
                o.getTotalAmount(),
                shipping,
                lines
        );
    }
}
