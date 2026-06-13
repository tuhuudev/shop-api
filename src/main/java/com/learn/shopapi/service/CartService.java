package com.learn.shopapi.service;

import com.learn.shopapi.dto.CartResponse;
import com.learn.shopapi.dto.OrderRequest;
import com.learn.shopapi.dto.OrderResponse;
import com.learn.shopapi.dto.ShippingAddressRequest;
import com.learn.shopapi.entity.Cart;
import com.learn.shopapi.entity.CartItem;
import com.learn.shopapi.entity.Product;
import com.learn.shopapi.entity.User;
import com.learn.shopapi.exception.ResourceNotFoundException;
import com.learn.shopapi.repository.CartRepository;
import com.learn.shopapi.repository.ProductRepository;
import com.learn.shopapi.repository.UserRepository;
import com.learn.shopapi.security.SecurityUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Gio hang cua nguoi dung dang dang nhap: xem, them/sua/xoa dong, va CHECKOUT.
 * Checkout tai su dung OrderService.createOrder (tru kho + tinh tien + transaction).
 */
@Service
public class CartService {

    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderService orderService;

    public CartService(CartRepository cartRepository, UserRepository userRepository,
                       ProductRepository productRepository, OrderService orderService) {
        this.cartRepository = cartRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.orderService = orderService;
    }

    @Transactional(readOnly = true)
    public CartResponse view() {
        return cartRepository.findByUserUsername(currentUsername())
                .map(CartResponse::from)
                .orElse(new CartResponse(List.of(), BigDecimal.ZERO));  // chua co gio -> rong
    }

    @Transactional
    public CartResponse addItem(Long productId, int quantity) {
        Cart cart = getOrCreateCart();
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay san pham id=" + productId));
        CartItem existing = cart.findItem(productId);
        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + quantity);   // cong don
        } else {
            cart.addItem(new CartItem(product, quantity));
        }
        return CartResponse.from(cart);
    }

    @Transactional
    public CartResponse updateItem(Long productId, int quantity) {
        Cart cart = getOrCreateCart();
        CartItem item = cart.findItem(productId);
        if (item == null) {
            throw new ResourceNotFoundException("San pham id=" + productId + " khong co trong gio");
        }
        if (quantity <= 0) {
            cart.removeItem(item);   // so luong 0 -> bo khoi gio
        } else {
            item.setQuantity(quantity);
        }
        return CartResponse.from(cart);
    }

    @Transactional
    public CartResponse removeItem(Long productId) {
        Cart cart = getOrCreateCart();
        CartItem item = cart.findItem(productId);
        if (item != null) {
            cart.removeItem(item);
        }
        return CartResponse.from(cart);
    }

    /** Bien gio hang thanh don hang (tru kho, ap coupon, chup dia chi giao), roi xoa rong gio. */
    @Transactional
    public OrderResponse checkout(String couponCode, ShippingAddressRequest shipping) {
        Cart cart = getOrCreateCart();
        if (cart.getItems().isEmpty()) {
            throw new IllegalArgumentException("Gio hang dang trong");
        }
        List<OrderRequest.OrderLine> lines = cart.getItems().stream()
                .map(i -> new OrderRequest.OrderLine(i.getProduct().getId(), i.getQuantity()))
                .toList();
        // customerId = null -> OrderService tu lay khach tu tai khoan dang dang nhap.
        OrderResponse order = orderService.createOrder(new OrderRequest(null, lines, couponCode, shipping));
        cart.getItems().clear();   // orphanRemoval = true -> xoa cac cart_items
        return order;
    }

    // ---- helper ----

    private Cart getOrCreateCart() {
        String username = currentUsername();
        return cartRepository.findByUserUsername(username)
                .orElseGet(() -> {
                    User user = userRepository.findByUsername(username)
                            .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay user: " + username));
                    return cartRepository.save(new Cart(user));
                });
    }

    private String currentUsername() {
        return SecurityUtils.getCurrentUsername()
                .orElseThrow(() -> new AccessDeniedException("Chua dang nhap"));
    }
}
