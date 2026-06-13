package com.learn.shopapi.service;

import com.learn.shopapi.dto.OrderRequest;
import com.learn.shopapi.dto.OrderResponse;
import com.learn.shopapi.dto.PageResponse;
import com.learn.shopapi.entity.*;
import com.learn.shopapi.exception.ResourceNotFoundException;
import com.learn.shopapi.repository.CustomerRepository;
import com.learn.shopapi.repository.OrderRepository;
import com.learn.shopapi.repository.ProductRepository;
import com.learn.shopapi.security.SecurityUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;

    public OrderService(OrderRepository orderRepository,
                        CustomerRepository customerRepository,
                        ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
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

        for (OrderRequest.OrderLine line : req.items()) {
            Product product = productRepository.findById(line.productId())
                    .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay san pham id=" + line.productId()));

            if (product.getStockQuantity() < line.quantity()) {
                throw new IllegalArgumentException(
                        "San pham '" + product.getName() + "' chi con " + product.getStockQuantity() + " trong kho");
            }
            // tru ton kho
            product.setStockQuantity(product.getStockQuantity() - line.quantity());

            order.addItem(new OrderItem(product, line.quantity()));
        }

        return OrderResponse.from(orderRepository.save(order));
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
        // Huy don -> HOAN KHO so luong da tru luc tao don.
        if (newStatus == OrderStatus.CANCELLED) {
            restock(order);
        }
        order.setStatus(newStatus);
        return OrderResponse.from(order);
    }

    /** Cong tra so luong cua tung dong hang ve ton kho (khi huy don). */
    private void restock(Order order) {
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
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
