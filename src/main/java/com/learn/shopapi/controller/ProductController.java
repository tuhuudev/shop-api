package com.learn.shopapi.controller;

import com.learn.shopapi.dto.PageResponse;
import com.learn.shopapi.dto.ProductRequest;
import com.learn.shopapi.dto.ProductResponse;
import com.learn.shopapi.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * REST controller cho san pham.
 *   GET    /api/products        -> danh sach CO PHAN TRANG + LOC (cong khai)
 *   GET    /api/products/{id}   -> chi tiet (cong khai)
 *   POST   /api/products        -> tao moi   (can quyen: STAFF/ADMIN)
 *   PUT    /api/products/{id}   -> cap nhat  (can quyen: STAFF/ADMIN)
 *   DELETE /api/products/{id}   -> xoa mem   (can quyen: STAFF/ADMIN)
 *
 * Endpoint xem (GET) la cong khai (cau hinh o SecurityConfig). Endpoint ghi
 * duoc chan bang @PreAuthorize ngay tai method (method security).
 */
@RestController
@RequestMapping("/api/products")
@Tag(name = "Products", description = "Quan ly san pham")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    @Operation(summary = "Danh sach san pham (phan trang + loc theo ten/gia/danh muc)")
    public PageResponse<ProductResponse> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Long categoryId,
            Pageable pageable) {
        return productService.find(search, minPrice, maxPrice, categoryId, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiet 1 san pham")
    public ProductResponse get(@PathVariable Long id) {
        return productService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Tao san pham moi (STAFF/ADMIN)")
    public ProductResponse create(@Valid @RequestBody ProductRequest request) {
        return productService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Cap nhat san pham (STAFF/ADMIN)")
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return productService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Xoa mem san pham (STAFF/ADMIN)")
    public void delete(@PathVariable Long id) {
        productService.delete(id);
    }
}
