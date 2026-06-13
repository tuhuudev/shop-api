package com.learn.shopapi.controller;

import com.learn.shopapi.dto.CategoryRequest;
import com.learn.shopapi.dto.CategoryResponse;
import com.learn.shopapi.dto.PageResponse;
import com.learn.shopapi.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * CRUD danh muc. Xem cong khai; tao/sua/xoa can STAFF/ADMIN (giong ProductController).
 */
@RestController
@RequestMapping("/api/categories")
@Tag(name = "Categories", description = "Quan ly danh muc")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    @Operation(summary = "Danh sach danh muc (phan trang)")
    public PageResponse<CategoryResponse> list(Pageable pageable) {
        return categoryService.findAll(pageable);
    }

    @GetMapping("/all")
    @Operation(summary = "Tat ca danh muc (cache, cho dropdown)")
    public List<CategoryResponse> all() {
        return categoryService.findAllCached();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiet danh muc")
    public CategoryResponse get(@PathVariable Long id) {
        return categoryService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Tao danh muc (STAFF/ADMIN)")
    public CategoryResponse create(@Valid @RequestBody CategoryRequest request) {
        return categoryService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Cap nhat danh muc (STAFF/ADMIN)")
    public CategoryResponse update(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        return categoryService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Xoa danh muc (STAFF/ADMIN)")
    public void delete(@PathVariable Long id) {
        categoryService.delete(id);
    }
}
