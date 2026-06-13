package com.learn.shopapi.dto;

import java.math.BigDecimal;

/** Bao cao doanh thu theo tung ngay. */
public interface DailyRevenueView {
    String getDay();
    Long getOrderCount();
    BigDecimal getRevenue();
}
