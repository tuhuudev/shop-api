package com.learn.shopapi.service;

import com.learn.shopapi.dto.OrderRequest;
import com.learn.shopapi.dto.OrderResponse;
import com.learn.shopapi.entity.Customer;
import com.learn.shopapi.entity.Order;
import com.learn.shopapi.entity.OrderItem;
import com.learn.shopapi.entity.OrderStatus;
import com.learn.shopapi.entity.Product;
import com.learn.shopapi.repository.CustomerRepository;
import com.learn.shopapi.repository.OrderRepository;
import com.learn.shopapi.repository.ProductRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit test cho OrderService - tap trung vao logic NGHIEP VU (tru kho, het hang).
 * Dung Mockito de gia lap repository: KHONG dung DB that -> chay nhanh, chi kiem logic.
 *
 * Vi createOrder doc "ai dang dang nhap" tu SecurityContext, ta dat san 1 nguoi dung
 * vai tro CUSTOMER truoc moi test.
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private ProductRepository productRepository;

    @InjectMocks private OrderService orderService;

    @BeforeEach
    void setUpSecurityContext() {
        var auth = new UsernamePasswordAuthenticationToken(
                "customer", null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createOrder_duKho_truKhoVaLuuDon() {
        Customer an = new Customer("An", "an@example.com");
        Product kb = new Product("Ban phim", "desc", new BigDecimal("100"), 10, null);
        when(customerRepository.findByUserUsername("customer")).thenReturn(Optional.of(an));
        when(productRepository.findById(1L)).thenReturn(Optional.of(kb));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderRequest req = new OrderRequest(null,
                List.of(new OrderRequest.OrderLine(1L, 3)));

        OrderResponse res = orderService.createOrder(req);

        assertThat(kb.getStockQuantity()).isEqualTo(7);               // 10 - 3
        assertThat(res.totalAmount()).isEqualByComparingTo("300");    // 100 * 3
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void createOrder_hetKho_nemLoiVaKhongLuu() {
        Customer an = new Customer("An", "an@example.com");
        Product kb = new Product("Ban phim", "desc", new BigDecimal("100"), 2, null);
        when(customerRepository.findByUserUsername("customer")).thenReturn(Optional.of(an));
        when(productRepository.findById(1L)).thenReturn(Optional.of(kb));

        OrderRequest req = new OrderRequest(null,
                List.of(new OrderRequest.OrderLine(1L, 5)));

        assertThatThrownBy(() -> orderService.createOrder(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("chi con 2");

        assertThat(kb.getStockQuantity()).isEqualTo(2);   // khong bi tru
        verify(orderRepository, never()).save(any());     // khong luu don nao
    }

    @Test
    void updateStatus_chuyenHopLe_PENDING_sang_PAID() {
        Order order = new Order(new Customer("An", "an@example.com"));   // mac dinh PENDING
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        OrderResponse res = orderService.updateStatus(1L, OrderStatus.PAID);

        assertThat(res.status()).isEqualTo("PAID");
    }

    @Test
    void updateStatus_chuyenSai_SHIPPED_ve_PENDING_nemLoi() {
        Order order = new Order(new Customer("An", "an@example.com"));
        order.setStatus(OrderStatus.SHIPPED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateStatus(1L, OrderStatus.PENDING))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Khong the chuyen trang thai");
    }

    @Test
    void huyDon_hoanLaiKho() {
        Product kb = new Product("Ban phim", "desc", new BigDecimal("100"), 5, null);
        Order order = new Order(new Customer("An", "an@example.com"));
        order.setStatus(OrderStatus.PAID);
        order.addItem(new OrderItem(kb, 2));   // don mua 2 (kho mo phong van la 5 trong test nay)
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        orderService.updateStatus(1L, OrderStatus.CANCELLED);

        assertThat(kb.getStockQuantity()).isEqualTo(7);   // 5 + 2 hoan lai
    }
}
