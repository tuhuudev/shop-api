package com.learn.shopapi.repository;

import com.learn.shopapi.dto.CategoryRevenueView;
import com.learn.shopapi.dto.CustomerSpendingView;
import com.learn.shopapi.dto.DailyRevenueView;
import com.learn.shopapi.dto.MonthlyRevenueView;
import com.learn.shopapi.dto.ProductSalesView;
import com.learn.shopapi.entity.Order;
import com.learn.shopapi.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByStatus(OrderStatus status);

    List<Order> findByCustomerId(Long customerId);

    // Phan trang don hang cua RIENG 1 tai khoan (CUSTOMER chi xem don cua minh).
    Page<Order> findByCustomerUserUsername(String username, Pageable pageable);

    /**
     * BAO CAO 1: San pham ban chay nhat.
     * Day la NATIVE SQL (SQL that su, chay thang tren DB) -> hoc JOIN + GROUP BY + SUM.
     * Chi tinh tren don da thanh toan tro len (khong tinh don da huy/cho).
     */
    @Query(value = """
            SELECT p.name              AS productName,
                   SUM(oi.quantity)    AS totalQuantity,
                   SUM(oi.quantity * oi.unit_price) AS totalRevenue
            FROM order_items oi
            JOIN products p ON p.id = oi.product_id
            JOIN orders   o ON o.id = oi.order_id
            WHERE o.status <> 'CANCELLED'
            GROUP BY p.id, p.name
            ORDER BY totalRevenue DESC
            """, nativeQuery = true)
    List<ProductSalesView> findBestSellingProducts();

    /**
     * BAO CAO 2: Doanh thu theo ngay (Postgres: to_char cat phan ngay yyyy-MM-dd).
     * DAY/MONTH la tu khoa reserved -> quote alias "day".
     */
    @Query(value = """
            SELECT to_char(o.order_date, 'YYYY-MM-DD') AS "day",
                   COUNT(o.id)            AS orderCount,
                   SUM(o.total_amount)    AS revenue
            FROM orders o
            WHERE o.status <> 'CANCELLED'
            GROUP BY to_char(o.order_date, 'YYYY-MM-DD')
            ORDER BY "day"
            """, nativeQuery = true)
    List<DailyRevenueView> findDailyRevenue();

    /**
     * BAO CAO 3: Tong doanh thu (mot con so).
     * COALESCE tra ve 0 neu chua co don nao (tranh null).
     */
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status <> :excluded")
    java.math.BigDecimal totalRevenueExcluding(@Param("excluded") OrderStatus excluded);

    /** BAO CAO: doanh thu theo DANH MUC (JOIN 4 bang). */
    @Query(value = """
            SELECT c.name              AS categoryName,
                   SUM(oi.quantity)    AS totalQuantity,
                   SUM(oi.quantity * oi.unit_price) AS totalRevenue
            FROM order_items oi
            JOIN products   p ON p.id = oi.product_id
            JOIN categories c ON c.id = p.category_id
            JOIN orders     o ON o.id = oi.order_id
            WHERE o.status <> 'CANCELLED'
            GROUP BY c.id, c.name
            ORDER BY totalRevenue DESC
            """, nativeQuery = true)
    List<CategoryRevenueView> findRevenueByCategory();

    /**
     * BAO CAO: doanh thu theo THANG (Postgres: to_char ...'YYYY-MM').
     * MONTH la tu khoa reserved -> quote alias "month".
     */
    @Query(value = """
            SELECT to_char(o.order_date, 'YYYY-MM') AS "month",
                   COUNT(o.id)         AS orderCount,
                   SUM(o.total_amount) AS revenue
            FROM orders o
            WHERE o.status <> 'CANCELLED'
            GROUP BY to_char(o.order_date, 'YYYY-MM')
            ORDER BY "month"
            """, nativeQuery = true)
    List<MonthlyRevenueView> findRevenueByMonth();

    /** BAO CAO: khach chi tieu nhieu nhat (top N truyen qua LIMIT cua Spring/Pageable thi phuc tap;
        o day lay tat ca, sap xep giam dan - client co the cat). */
    @Query(value = """
            SELECT cu.name             AS customerName,
                   COUNT(o.id)         AS orderCount,
                   SUM(o.total_amount) AS totalSpent
            FROM orders o
            JOIN customers cu ON cu.id = o.customer_id
            WHERE o.status <> 'CANCELLED'
            GROUP BY cu.id, cu.name
            ORDER BY totalSpent DESC
            """, nativeQuery = true)
    List<CustomerSpendingView> findTopCustomers();
}
