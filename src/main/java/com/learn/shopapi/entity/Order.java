package com.learn.shopapi.entity;

import com.learn.shopapi.common.Auditable;
import jakarta.persistence.*;
import org.hibernate.annotations.BatchSize;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Order = mot don hang.
 * Luu y: bang ten "orders" vi ORDER la tu khoa reserved trong SQL.
 *
 * - 1 Order thuoc 1 Customer            -> Many-to-One
 * - 1 Order co nhieu dong hang OrderItem -> One-to-Many
 * - Ke thua Auditable: tu co created_at/created_by (ai tao don, luc nao).
 */
@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_orders_customer", columnList = "customer_id"),
        @Index(name = "idx_orders_order_date", columnList = "order_date")
})
public class Order extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(nullable = false)
    private LocalDateTime orderDate;

    @Enumerated(EnumType.STRING) // luu chuoi "PENDING" thay vi so 0,1,2 -> de doc
    @Column(nullable = false)
    private OrderStatus status;

    // cascade = ALL: luu/xoa Order thi cac OrderItem cung tu dong theo.
    // orphanRemoval = true: xoa item khoi list -> xoa luon trong DB.
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    // Batch loading: nap items cua nhieu Order trong 1 trang bang vai cau IN (chong N+1).
    @BatchSize(size = 100)
    private List<OrderItem> items = new ArrayList<>();

    // So tien giam tu coupon (0 neu khong dung). totalAmount = tong dong hang - discountAmount.
    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "coupon_code", length = 64)
    private String couponCode;

    // Dia chi giao hang chup luc tao don (cac cot ship_* tren bang orders, deu nullable).
    @Embedded
    private ShippingAddress shippingAddress;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    // Optimistic locking: chong 2 thao tac doi trang thai don dong thoi ghi de nhau.
    @Version
    private Long version;

    protected Order() { }

    public Order(Customer customer) {
        this.customer = customer;
        this.orderDate = LocalDateTime.now();
        this.status = OrderStatus.PENDING;
    }

    /** Them 1 dong hang va cap nhat tong tien. Giu 2 chieu quan he dong bo. */
    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
        recalculateTotal();
    }

    /** Tong tien cac dong hang (truoc giam gia). */
    public BigDecimal subtotal() {
        return items.stream()
                .map(OrderItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void recalculateTotal() {
        this.totalAmount = subtotal().subtract(discountAmount);
    }

    /** Ap giam gia: luu so tien giam + ma coupon, tinh lai tong phai tra. */
    public void applyDiscount(String couponCode, BigDecimal discountAmount) {
        this.couponCode = couponCode;
        this.discountAmount = discountAmount;
        recalculateTotal();
    }

    public Long getId() { return id; }
    public Customer getCustomer() { return customer; }
    public LocalDateTime getOrderDate() { return orderDate; }
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
    public List<OrderItem> getItems() { return items; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public String getCouponCode() { return couponCode; }
    public ShippingAddress getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(ShippingAddress shippingAddress) { this.shippingAddress = shippingAddress; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public Long getVersion() { return version; }
}
