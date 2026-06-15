package com.learn.shopapi.controller;

import com.learn.shopapi.dto.CartItemRequest;
import com.learn.shopapi.dto.CartResponse;
import com.learn.shopapi.dto.CheckoutRequest;
import com.learn.shopapi.dto.OrderResponse;
import com.learn.shopapi.service.CartService;
import com.learn.shopapi.service.IdempotencyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Gio hang cua nguoi dung dang dang nhap. Moi thao tac tren gio cua CHINH MINH.
 */
@RestController
@RequestMapping("/api/cart")
@PreAuthorize("isAuthenticated()")
@Tag(name = "Cart", description = "Gio hang")
@SecurityRequirement(name = "bearerAuth")
public class CartController {

    private final CartService cartService;
    private final IdempotencyService idempotencyService;

    public CartController(CartService cartService,
                          IdempotencyService idempotencyService) {
        this.cartService = cartService;
        this.idempotencyService = idempotencyService;
    }

    @GetMapping
    @Operation(summary = "Xem gio hang")
    public CartResponse view() {
        return cartService.view();
    }

    @PostMapping("/items")
    @Operation(summary = "Them san pham vao gio (cong don neu da co)")
    public CartResponse addItem(@Valid @RequestBody CartItemRequest request) {
        return cartService.addItem(request.productId(), request.quantity());
    }

    @PutMapping("/items/{productId}")
    @Operation(summary = "Dat lai so luong 1 san pham (<=0 thi xoa khoi gio)")
    public CartResponse updateItem(@PathVariable Long productId, @RequestParam int quantity) {
        return cartService.updateItem(productId, quantity);
    }

    @DeleteMapping("/items/{productId}")
    @Operation(summary = "Xoa 1 san pham khoi gio")
    public CartResponse removeItem(@PathVariable Long productId) {
        return cartService.removeItem(productId);
    }

    @PostMapping("/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Dat hang tu gio (couponCode + dia chi giao tuy chon; Idempotency-Key de retry an toan)")
    public OrderResponse checkout(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody(required = false) @Valid CheckoutRequest body) {
        CheckoutRequest req = (body != null) ? body : new CheckoutRequest(null, null);
        return idempotencyService.execute(idempotencyKey,
                () -> cartService.checkout(req.couponCode(), req.shipping()));
    }
}
