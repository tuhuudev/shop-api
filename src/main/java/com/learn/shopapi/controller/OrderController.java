package com.learn.shopapi.controller;

import com.learn.shopapi.dto.OrderRequest;
import com.learn.shopapi.dto.OrderResponse;
import com.learn.shopapi.dto.PageResponse;
import com.learn.shopapi.entity.OrderStatus;
import com.learn.shopapi.service.IdempotencyService;
import com.learn.shopapi.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Don hang. TAT CA endpoint deu can dang nhap.
 * - list/get/create: moi nguoi da dang nhap (CUSTOMER chi thay/dat don cua minh - xu ly o service).
 * - updateStatus: chi STAFF/ADMIN.
 */
@RestController
@RequestMapping("/api/orders")
@Tag(name = "Orders", description = "Dat hang va theo doi don")
@SecurityRequirement(name = "bearerAuth")
public class OrderController {

    private final OrderService orderService;
    private final IdempotencyService idempotencyService;

    public OrderController(OrderService orderService,
                           IdempotencyService idempotencyService) {
        this.orderService = orderService;
        this.idempotencyService = idempotencyService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Danh sach don (phan trang). CUSTOMER chi thay don cua minh")
    public PageResponse<OrderResponse> list(Pageable pageable) {
        return orderService.findAll(pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Chi tiet don (CUSTOMER chi xem don cua minh)")
    public OrderResponse get(@PathVariable Long id) {
        return orderService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Tao don (ho tro header Idempotency-Key de retry an toan, khong tao trung)")
    public OrderResponse create(@Valid @RequestBody OrderRequest request,
                                @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return idempotencyService.execute(idempotencyKey, () -> orderService.createOrder(request));
    }

    @PostMapping("/{id}/pay")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Thanh toan don (MOCK cong thanh toan) -> chuyen PENDING sang PAID")
    public OrderResponse pay(@PathVariable Long id) {
        return orderService.pay(id);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Khach tu huy don cua minh khi con PENDING (hoan kho)")
    public OrderResponse cancel(@PathVariable Long id) {
        return orderService.cancelOwnOrder(id);
    }

    // PATCH chi sua mot phan (o day la trang thai). Vi du body: {"status":"PAID"}
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    @Operation(summary = "Doi trang thai don (STAFF/ADMIN)")
    public OrderResponse updateStatus(@PathVariable Long id, @RequestBody StatusUpdate body) {
        return orderService.updateStatus(id, body.status());
    }

    public record StatusUpdate(OrderStatus status) { }
}
