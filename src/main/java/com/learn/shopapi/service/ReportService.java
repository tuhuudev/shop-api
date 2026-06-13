package com.learn.shopapi.service;

import com.learn.shopapi.dto.CategoryRevenueView;
import com.learn.shopapi.dto.CustomerSpendingView;
import com.learn.shopapi.dto.DailyRevenueView;
import com.learn.shopapi.dto.MonthlyRevenueView;
import com.learn.shopapi.dto.ProductSalesView;
import com.learn.shopapi.entity.OrderStatus;
import com.learn.shopapi.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/** Tap hop cac bao cao phan tich du lieu (phan "Data"). */
@Service
public class ReportService {

    private final OrderRepository orderRepository;

    public ReportService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public List<ProductSalesView> bestSellers() {
        return orderRepository.findBestSellingProducts();
    }

    @Transactional(readOnly = true)
    public List<DailyRevenueView> dailyRevenue() {
        return orderRepository.findDailyRevenue();
    }

    @Transactional(readOnly = true)
    public BigDecimal totalRevenue() {
        return orderRepository.totalRevenueExcluding(OrderStatus.CANCELLED);
    }

    @Transactional(readOnly = true)
    public List<CategoryRevenueView> revenueByCategory() {
        return orderRepository.findRevenueByCategory();
    }

    @Transactional(readOnly = true)
    public List<MonthlyRevenueView> revenueByMonth() {
        return orderRepository.findRevenueByMonth();
    }

    @Transactional(readOnly = true)
    public List<CustomerSpendingView> topCustomers() {
        return orderRepository.findTopCustomers();
    }
}
