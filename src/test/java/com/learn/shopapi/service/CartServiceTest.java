package com.learn.shopapi.service;

import com.learn.shopapi.dto.CartResponse;
import com.learn.shopapi.dto.OrderRequest;
import com.learn.shopapi.dto.OrderResponse;
import com.learn.shopapi.entity.Cart;
import com.learn.shopapi.entity.CartItem;
import com.learn.shopapi.entity.Product;
import com.learn.shopapi.entity.User;
import com.learn.shopapi.exception.ResourceNotFoundException;
import com.learn.shopapi.repository.CartRepository;
import com.learn.shopapi.repository.ProductRepository;
import com.learn.shopapi.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Unit test CartService: them/sua/xoa dong, tong tien, checkout chuyen sang OrderService roi xoa gio. */
@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock private CartRepository cartRepository;
    @Mock private UserRepository userRepository;
    @Mock private ProductRepository productRepository;
    @Mock private OrderService orderService;
    @InjectMocks private CartService cartService;

    private final User alice = new User("alice", "{hash}", "alice@mail.vn", null);
    private Cart cart;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("alice", null, List.of()));
        cart = new Cart(alice);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private static Product product(long id, String price) {
        Product p = new Product("SP" + id, null, new BigDecimal(price), 10, null);
        ReflectionTestUtils.setField(p, "id", id);
        return p;
    }

    private void existingCart() {
        when(cartRepository.findByUserUsername("alice")).thenReturn(Optional.of(cart));
    }

    @Test
    void view_chuaCoGio_traGioRong() {
        when(cartRepository.findByUserUsername("alice")).thenReturn(Optional.empty());

        CartResponse res = cartService.view();

        assertThat(res.items()).isEmpty();
        assertThat(res.totalAmount()).isEqualByComparingTo("0");
    }

    @Test
    void addItem_chuaCoGio_taoGioMoiChoUser() {
        when(cartRepository.findByUserUsername("alice")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product(1, "100.00")));

        CartResponse res = cartService.addItem(1L, 2);

        ArgumentCaptor<Cart> saved = ArgumentCaptor.forClass(Cart.class);
        verify(cartRepository).save(saved.capture());
        assertThat(saved.getValue().getUser()).isSameAs(alice);
        assertThat(res.items()).singleElement().satisfies(l -> {
            assertThat(l.quantity()).isEqualTo(2);
            assertThat(l.lineTotal()).isEqualByComparingTo("200.00");
        });
    }

    @Test
    void addItem_daCoTrongGio_congDonSoLuong_tinhTongDung() {
        existingCart();
        Product p1 = product(1, "100.00");
        cart.addItem(new CartItem(p1, 1));
        cart.addItem(new CartItem(product(2, "15.50"), 2));
        when(productRepository.findById(1L)).thenReturn(Optional.of(p1));

        CartResponse res = cartService.addItem(1L, 3);

        assertThat(res.items()).hasSize(2);
        assertThat(cart.findItem(1L).getQuantity()).isEqualTo(4);
        assertThat(res.totalAmount()).isEqualByComparingTo("431.00");   // 4*100 + 2*15.50
    }

    @Test
    void addItem_sanPhamKhongTonTai_404() {
        existingCart();
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.addItem(99L, 1)).isInstanceOf(ResourceNotFoundException.class);
        assertThat(cart.getItems()).isEmpty();
    }

    @Test
    void updateItem_soLuong0_boKhoiGio() {
        existingCart();
        cart.addItem(new CartItem(product(1, "100.00"), 3));

        CartResponse res = cartService.updateItem(1L, 0);

        assertThat(res.items()).isEmpty();
    }

    @Test
    void updateItem_khongCoTrongGio_404() {
        existingCart();

        assertThatThrownBy(() -> cartService.updateItem(1L, 2)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void removeItem_khongCo_khongLoi() {
        existingCart();
        cart.addItem(new CartItem(product(1, "100.00"), 1));

        CartResponse res = cartService.removeItem(2L);

        assertThat(res.items()).hasSize(1);
    }

    @Test
    void checkout_gioRong_tuChoi_khongTaoDon() {
        existingCart();

        assertThatThrownBy(() -> cartService.checkout(null, null)).isInstanceOf(IllegalArgumentException.class);
        verify(orderService, never()).createOrder(any());
    }

    @Test
    void checkout_chuyenDungDongHangVaCoupon_roiXoaGio() {
        existingCart();
        cart.addItem(new CartItem(product(1, "100.00"), 2));
        cart.addItem(new CartItem(product(2, "15.50"), 1));
        OrderResponse order = mock(OrderResponse.class);
        when(orderService.createOrder(any())).thenReturn(order);

        OrderResponse res = cartService.checkout("SALE10", null);

        ArgumentCaptor<OrderRequest> req = ArgumentCaptor.forClass(OrderRequest.class);
        verify(orderService).createOrder(req.capture());
        assertThat(req.getValue().customerId()).isNull();
        assertThat(req.getValue().couponCode()).isEqualTo("SALE10");
        assertThat(req.getValue().items()).containsExactlyInAnyOrder(
                new OrderRequest.OrderLine(1L, 2), new OrderRequest.OrderLine(2L, 1));
        assertThat(res).isSameAs(order);
        assertThat(cart.getItems()).isEmpty();
    }

    @Test
    void chuaDangNhap_tuChoi() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> cartService.view()).isInstanceOf(AccessDeniedException.class);
    }
}
