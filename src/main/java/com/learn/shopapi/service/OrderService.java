package com.learn.shopapi.service;

import com.learn.shopapi.dto.OrderRequest;
import com.learn.shopapi.dto.OrderResponse;
import com.learn.shopapi.dto.PageResponse;
import com.learn.shopapi.entity.*;
import com.learn.shopapi.exception.ResourceNotFoundException;
import com.learn.shopapi.repository.CustomerRepository;
import com.learn.shopapi.repository.InventoryMovementRepository;
import com.learn.shopapi.repository.OrderRepository;
import com.learn.shopapi.repository.ProductRepository;
import com.learn.shopapi.security.SecurityUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final InventoryMovementRepository inventoryMovementRepository;
    private final CouponService couponService;

    public OrderService(OrderRepository orderRepository,
                        CustomerRepository customerRepository,
                        ProductRepository productRepository,
                        InventoryMovementRepository inventoryMovementRepository,
                        CouponService couponService) {
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.inventoryMovementRepository = inventoryMovementRepository;
        this.couponService = couponService;
    }

    /**
     * Tao don hang. Day la vi du dien hinh ve TRANSACTION:
     * tru kho + tao don phai cung thanh cong hoac cung that bai.
     * Neu mot san pham het hang -> nem loi -> JPA tu rollback toan bo.
     *
     * Phan quyen: KHACH (CUSTOMER) chi dat don cho CHINH MINH (lay Customer tu tai khoan dang nhap,
     * bo qua customerId client gui). STAFF/ADMIN duoc phep dat ho khach khac qua customerId.
     */
    @Transactional
    public OrderResponse createOrder(OrderRequest req) {
        Customer customer = resolveCustomer(req.customerId());
        Order order = new Order(customer);

        // Chup dia chi giao hang vao don (neu client gui).
        if (req.shipping() != null) {
            var s = req.shipping();
            order.setShippingAddress(new ShippingAddress(
                    s.recipient(), s.phone(), s.line1(), s.line2(),
                    s.city(), s.province(), s.postalCode(), s.country()));
        }

        for (OrderRequest.OrderLine line : req.items()) {
            Product product = productRepository.findById(line.productId())
                    .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay san pham id=" + line.productId()));

            // Tru kho ATOMIC: dieu kien du kho nam trong UPD...WHERE stock>=qty -> khong race,
            // khong can optimistic-lock/retry. updated==0 => khong du ton kho.
            if (productRepository.decrementStock(product.getId(), line.quantity()) == 0) {
                throw new IllegalArgumentException(
                        "San pham '" + product.getName() + "' khong du ton kho");
            }
            order.addItem(new OrderItem(product, line.quantity()));
        }

        // Ap coupon (neu co) tren subtotal -> set discount + tru luot ATOMIC trong cung tx.
        if (req.couponCode() != null && !req.couponCode().isBlank()) {
            String code = req.couponCode().trim();
            order.applyDiscount(code, couponService.applyToSubtotal(code, order.subtotal()));
        }

        Order saved = orderRepository.save(order);
        // Ghi nhat ky xuat kho (ORDER_OUT) sau khi co order id.
        String actor = SecurityUtils.getCurrentUsername().orElse("system");
        for (OrderItem item : saved.getItems()) {
            inventoryMovementRepository.save(new InventoryMovement(
                    item.getProduct().getId(), -item.getQuantity(),
                    InventoryMovementReason.ORDER_OUT, saved.getId(), actor));
        }
        return OrderResponse.from(saved);
    }

    /**
     * Danh sach don (phan trang). STAFF/ADMIN thay TAT CA; CUSTOMER chi thay don cua minh.
     */
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> findAll(Pageable pageable) {
        if (SecurityUtils.hasAnyRole("STAFF", "ADMIN")) {
            return PageResponse.from(orderRepository.findAll(pageable), OrderResponse::from);
        }
        String username = currentUsername();
        return PageResponse.from(
                orderRepository.findByCustomerUserUsername(username, pageable), OrderResponse::from);
    }

    @Transactional(readOnly = true)
    public OrderResponse findById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay don hang id=" + id));
        ensureCanView(order);
        return OrderResponse.from(order);
    }

    @Transactional
    public OrderResponse updateStatus(Long id, OrderStatus newStatus) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay don hang id=" + id));

        OrderStatus current = order.getStatus();
        // Chi cho chuyen theo luat (state machine trong OrderStatus). Nhay sai -> 400.
        if (!current.canTransitionTo(newStatus)) {
            throw new IllegalArgumentException(
                    "Khong the chuyen trang thai tu " + current + " sang " + newStatus);
        }
        // Huy/hoan tien -> HOAN KHO so luong da tru luc tao don.
        // An toan da-instance: @Version tren Order khien chi 1 tx chuyen trang thai thanh cong;
        // tx thua optimistic-lock se rollback CA lenh hoan kho -> khong bao gio hoan kho 2 lan.
        if (newStatus.isStockReturning()) {
            restock(order);
        }
        order.setStatus(newStatus);
        return OrderResponse.from(order);
    }

    /**
     * CUSTOMER tu huy don CUA MINH khi con PENDING (chua thanh toan). Hoan kho.
     * Don da PAID tro len phai di duong hoan tien (refund) do STAFF/ADMIN xu ly.
     */
    @Transactional
    public OrderResponse cancelOwnOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay don hang id=" + id));
        ensureCanView(order);   // chi chu don (hoac STAFF/ADMIN)
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalArgumentException(
                    "Chi huy duoc don dang cho thanh toan (PENDING); don hien o " + order.getStatus());
        }
        restock(order);
        order.setStatus(OrderStatus.CANCELLED);
        return OrderResponse.from(order);
    }

    /**
     * Tu dong huy 1 don PENDING qua han (job quet dinh ky). Chay tx RIENG (REQUIRES_NEW) cho tung don
     * de 1 don loi khong keo do ca me. Kiem tra lai trang thai + tuoi don ngay trong tx -> tranh
     * huy nham khi don vua duoc thanh toan. @Version dam bao multi-instance khong huy/hoan kho trung.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean autoCancelStale(Long id, java.time.LocalDateTime cutoff) {
        Order order = orderRepository.findById(id).orElse(null);
        if (order == null || order.getStatus() != OrderStatus.PENDING
                || !order.getOrderDate().isBefore(cutoff)) {
            return false;
        }
        restock(order);
        order.setStatus(OrderStatus.CANCELLED);
        return true;
    }

    /** Cong tra so luong cua tung dong hang ve ton kho (khi huy/hoan don) - ATOMIC tai DB + ghi nhat ky. */
    private void restock(Order order) {
        String actor = SecurityUtils.getCurrentUsername().orElse("system");
        for (OrderItem item : order.getItems()) {
            productRepository.incrementStock(item.getProduct().getId(), item.getQuantity());
            inventoryMovementRepository.save(new InventoryMovement(
                    item.getProduct().getId(), item.getQuantity(),
                    InventoryMovementReason.RESTOCK, order.getId(), actor));
        }
    }

    /**
     * MOCK thanh toan: mo phong cong thanh toan "thanh cong" -> chuyen PENDING sang PAID.
     * (Du an that se goi cong thanh toan that o day.) CUSTOMER chi tra cho don cua minh.
     */
    @Transactional
    public OrderResponse pay(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay don hang id=" + id));
        ensureCanView(order);
        if (!order.getStatus().canTransitionTo(OrderStatus.PAID)) {
            throw new IllegalArgumentException(
                    "Don o trang thai " + order.getStatus() + " khong the thanh toan");
        }
        order.setStatus(OrderStatus.PAID);
        return OrderResponse.from(order);
    }

    /**
     * Xac nhan da thanh toan tu CONG THANH TOAN (webhook) - KHONG kiem security context (server-to-server).
     * Idempotent: don da PAID tro len -> bo qua. Goi trong tx cua PaymentService.
     */
    @Transactional
    public void confirmPaid(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay don hang id=" + orderId));
        if (order.getStatus() == OrderStatus.PENDING) {
            order.setStatus(OrderStatus.PAID);
        }
        // Da PAID/khac -> khong lam gi (callback trung hoac don da xu ly).
    }

    // ---- helper ----

    /** Xac dinh khach hang cho don: STAFF/ADMIN co the chi dinh customerId; con lai lay tu tai khoan. */
    private Customer resolveCustomer(Long requestedCustomerId) {
        if (SecurityUtils.hasAnyRole("STAFF", "ADMIN") && requestedCustomerId != null) {
            return customerRepository.findById(requestedCustomerId)
                    .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay khach hang id=" + requestedCustomerId));
        }
        String username = currentUsername();
        return customerRepository.findByUserUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Tai khoan '" + username + "' chua co ho so khach hang de dat don"));
    }

    /** CUSTOMER chi duoc xem don cua chinh minh; STAFF/ADMIN xem tat ca. */
    private void ensureCanView(Order order) {
        if (SecurityUtils.hasAnyRole("STAFF", "ADMIN")) {
            return;
        }
        String username = currentUsername();
        User owner = order.getCustomer().getUser();
        if (owner == null || !username.equals(owner.getUsername())) {
            throw new AccessDeniedException("Ban khong duoc xem don hang nay");
        }
    }

    private String currentUsername() {
        return SecurityUtils.getCurrentUsername()
                .orElseThrow(() -> new AccessDeniedException("Chua dang nhap"));
    }
}
