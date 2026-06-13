package com.learn.shopapi.dto;

import java.math.BigDecimal;

/** Bao cao: doanh thu theo danh muc (JOIN order_items - products - categories). */
public interface CategoryRevenueView {
    String getCategoryName();
    Long getTotalQuantity();
    BigDecimal getTotalRevenue();
}
