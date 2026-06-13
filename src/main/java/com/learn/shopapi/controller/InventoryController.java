package com.learn.shopapi.controller;

import com.learn.shopapi.dto.InventoryMovementResponse;
import com.learn.shopapi.dto.PageResponse;
import com.learn.shopapi.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Doi soat ton kho (STAFF/ADMIN): xem nhat ky nhap/xuat kho. */
@RestController
@RequestMapping("/api/admin/inventory")
@PreAuthorize("hasAnyRole('STAFF','ADMIN')")
@Tag(name = "Inventory", description = "Nhat ky bien dong ton kho (STAFF/ADMIN)")
@SecurityRequirement(name = "bearerAuth")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/movements")
    @Operation(summary = "Nhat ky bien dong ton kho (loc theo productId neu co), phan trang")
    public PageResponse<InventoryMovementResponse> movements(
            @RequestParam(required = false) Long productId, Pageable pageable) {
        return inventoryService.movements(productId, pageable);
    }
}
