package com.learn.shopapi.controller;

import com.learn.shopapi.dto.CouponRequest;
import com.learn.shopapi.dto.CouponResponse;
import com.learn.shopapi.dto.PageResponse;
import com.learn.shopapi.service.CouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Quan tri coupon (ADMIN). */
@RestController
@RequestMapping("/api/admin/coupons")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Coupons", description = "Quan tri ma giam gia (ADMIN)")
@SecurityRequirement(name = "bearerAuth")
public class CouponController {

    private final CouponService couponService;

    public CouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    @GetMapping
    @Operation(summary = "Danh sach coupon (phan trang)")
    public PageResponse<CouponResponse> list(Pageable pageable) {
        return couponService.list(pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Tao coupon moi")
    public CouponResponse create(@Valid @RequestBody CouponRequest request) {
        return couponService.create(request);
    }
}
