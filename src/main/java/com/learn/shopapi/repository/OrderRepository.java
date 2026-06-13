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
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByStatus(OrderStatus status);

    List<Order> findByCustomerId(Long customerId);

    // Id cac don dang PENDING tao truoc moc thoi gian (job tu dong huy don qua han).
    @Query("SELECT o.id FROM Order o WHERE o.status = :status AND o.orderDate < :cutoff")
    List<Long> findIdsByStatusAndOrderDateBefore(@Param("status") OrderStatus status,
                                                 @Param("cutoff") java.time.LocalDateTime cutoff);

    // Chi tiet 1 don: fetch join customer + items + product trong 1 query (khong N+1).
    @Override
    @EntityGraph(attributePaths = {"customer", "items", "items.product"})
    Optional<Order> findById(Long id);

    // List phan trang: nap kem customer (ToOne, an toan voi paging); items/product nap theo
    // @BatchSize tren entity -> vai cau IN thay vi N+1. KHONG fetch collection o day de tranh
    // phan trang trong bo nho (HHH000104).
    @Override
    @EntityGraph(attributePaths = "customer")
    Page<Order> findAll(Pageable pageable);

    // Phan trang don hang cua RIENG 1 tai khoan (CUSTOMER chi xem don cua minh).
    @EntityGraph(attributePaths = "customer")
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
              AND o.order_date >= :from AND o.order_date < :toExclusive
            GROUP BY p.id, p.name
            ORDER BY totalRevenue DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<ProductSalesView> findBestSellingProducts(@Param("from") java.time.LocalDateTime from,
                                                   @Param("toExclusive") java.time.LocalDateTime toExclusive,
                                                   @Param("limit") int limit);

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
              AND o.order_date >= :from AND o.order_date < :toExclusive
            GROUP BY to_char(o.order_date, 'YYYY-MM-DD')
            ORDER BY "day"
            """, nativeQuery = true)
    List<DailyRevenueView> findDailyRevenue(@Param("from") java.time.LocalDateTime from,
                                            @Param("toExclusive") java.time.LocalDateTime toExclusive);

    /**
     * BAO CAO 3: Tong doanh thu (mot con so).
     * COALESCE tra ve 0 neu chua co don nao (tranh null).
     */
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status <> :excluded "
            + "AND o.orderDate >= :from AND o.orderDate < :toExclusive")
    java.math.BigDecimal totalRevenueExcluding(@Param("excluded") OrderStatus excluded,
                                               @Param("from") java.time.LocalDateTime from,
                                               @Param("toExclusive") java.time.LocalDateTime toExclusive);

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
              AND o.order_date >= :from AND o.order_date < :toExclusive
            GROUP BY c.id, c.name
            ORDER BY totalRevenue DESC
            """, nativeQuery = true)
    List<CategoryRevenueView> findRevenueByCategory(@Param("from") java.time.LocalDateTime from,
                                                    @Param("toExclusive") java.time.LocalDateTime toExclusive);

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
              AND o.order_date >= :from AND o.order_date < :toExclusive
            GROUP BY to_char(o.order_date, 'YYYY-MM')
            ORDER BY "month"
            """, nativeQuery = true)
    List<MonthlyRevenueView> findRevenueByMonth(@Param("from") java.time.LocalDateTime from,
                                                @Param("toExclusive") java.time.LocalDateTime toExclusive);

    /** BAO CAO: khach chi tieu nhieu nhat (top N qua LIMIT - tranh tra ve toan bo bang). */
    @Query(value = """
            SELECT cu.name             AS customerName,
                   COUNT(o.id)         AS orderCount,
                   SUM(o.total_amount) AS totalSpent
            FROM orders o
            JOIN customers cu ON cu.id = o.customer_id
            WHERE o.status <> 'CANCELLED'
              AND o.order_date >= :from AND o.order_date < :toExclusive
            GROUP BY cu.id, cu.name
            ORDER BY totalSpent DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<CustomerSpendingView> findTopCustomers(@Param("from") java.time.LocalDateTime from,
                                                @Param("toExclusive") java.time.LocalDateTime toExclusive,
                                                @Param("limit") int limit);
}
