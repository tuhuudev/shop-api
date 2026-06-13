package com.learn.shopapi.dto;

import java.math.BigDecimal;

/**
 * Projection = khung ket qua bao cao (chi cac cot ta can).
 * Spring tu map ket qua truy van vao cac getter nay theo TEN cot/alias.
 * Day la bao cao "san pham ban chay nhat".
 */
public interface ProductSalesView {
    String getProductName();
    Long getTotalQuantity();
    BigDecimal getTotalRevenue();
}
