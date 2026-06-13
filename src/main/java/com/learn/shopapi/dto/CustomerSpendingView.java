package com.learn.shopapi.dto;

import java.math.BigDecimal;

/** Bao cao: khach hang chi tieu nhieu nhat. */
public interface CustomerSpendingView {
    String getCustomerName();
    Long getOrderCount();
    BigDecimal getTotalSpent();
}
