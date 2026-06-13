package com.learn.shopapi.dto;

import java.math.BigDecimal;

/** Bao cao: doanh thu theo thang (yyyy-MM). */
public interface MonthlyRevenueView {
    String getMonth();
    Long getOrderCount();
    BigDecimal getRevenue();
}
