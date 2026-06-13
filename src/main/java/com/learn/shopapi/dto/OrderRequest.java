package com.learn.shopapi.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

/**
 * Du lieu tao don hang moi: (tuy chon) ai mua + danh sach (san pham, so luong).
 * customerId TUY CHON: chi STAFF/ADMIN dung de dat ho khach khac; CUSTOMER bo trong
 * (he thong tu lay tu tai khoan dang nhap - xem OrderService.resolveCustomer).
 */
public record OrderRequest(
        Long customerId,

        @NotEmpty(message = "Don hang phai co it nhat 1 san pham")
        List<@Valid OrderLine> items
) {
    public record OrderLine(
            @NotNull Long productId,
            @Positive(message = "So luong phai lon hon 0") int quantity
    ) { }
}
