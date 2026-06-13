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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Tap hop cac bao cao phan tich du lieu (phan "Data").
 *
 * Tat ca bao cao nhan khoang thoi gian [from, to] (theo NGAY, bao gom ca ngay 'to').
 * from/to = null -> mac dinh toan bo lich su (tu 2000-01-01 den hom nay) de tuong thich nguoc.
 */
@Service
public class ReportService {

    private static final LocalDate MIN_DATE = LocalDate.of(2000, 1, 1);

    private final OrderRepository orderRepository;

    public ReportService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public List<ProductSalesView> bestSellers(LocalDate from, LocalDate to, int limit) {
        return orderRepository.findBestSellingProducts(from(from), toExclusive(to), limit);
    }

    @Transactional(readOnly = true)
    public List<DailyRevenueView> dailyRevenue(LocalDate from, LocalDate to) {
        return orderRepository.findDailyRevenue(from(from), toExclusive(to));
    }

    @Transactional(readOnly = true)
    public BigDecimal totalRevenue(LocalDate from, LocalDate to) {
        return orderRepository.totalRevenueExcluding(OrderStatus.CANCELLED, from(from), toExclusive(to));
    }

    @Transactional(readOnly = true)
    public List<CategoryRevenueView> revenueByCategory(LocalDate from, LocalDate to) {
        return orderRepository.findRevenueByCategory(from(from), toExclusive(to));
    }

    @Transactional(readOnly = true)
    public List<MonthlyRevenueView> revenueByMonth(LocalDate from, LocalDate to) {
        return orderRepository.findRevenueByMonth(from(from), toExclusive(to));
    }

    @Transactional(readOnly = true)
    public List<CustomerSpendingView> topCustomers(LocalDate from, LocalDate to, int limit) {
        return orderRepository.findTopCustomers(from(from), toExclusive(to), limit);
    }

    // ---- chuan hoa khoang thoi gian ----

    /** Dau ngay 'from' (null -> moc rat cu). */
    private LocalDateTime from(LocalDate from) {
        return (from != null ? from : MIN_DATE).atStartOfDay();
    }

    /** Dau ngay LIEN SAU 'to' -> dieu kien "< toExclusive" bao gom tron ven ngay 'to'. */
    private LocalDateTime toExclusive(LocalDate to) {
        return (to != null ? to : LocalDate.now()).plusDays(1).atStartOfDay();
    }
}
