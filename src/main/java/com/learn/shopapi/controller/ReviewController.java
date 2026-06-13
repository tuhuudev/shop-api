package com.learn.shopapi.controller;

import com.learn.shopapi.dto.PageResponse;
import com.learn.shopapi.dto.ReviewRequest;
import com.learn.shopapi.dto.ReviewResponse;
import com.learn.shopapi.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Danh gia san pham. GET cong khai (nam duoi /api/products/** GET -> permitAll);
 * POST can dang nhap.
 */
@RestController
@RequestMapping("/api/products/{productId}/reviews")
@Tag(name = "Reviews", description = "Danh gia san pham")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping
    @Operation(summary = "Danh sach danh gia cua san pham (phan trang)")
    public PageResponse<ReviewResponse> list(@PathVariable Long productId, Pageable pageable) {
        return reviewService.listByProduct(productId, pageable);
    }

    @GetMapping("/summary")
    @Operation(summary = "Diem trung binh + so luot danh gia")
    public ReviewService.Summary summary(@PathVariable Long productId) {
        return reviewService.summary(productId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Gui danh gia (can dang nhap)")
    public ReviewResponse create(@PathVariable Long productId, @Valid @RequestBody ReviewRequest request) {
        return reviewService.addReview(productId, request);
    }
}
