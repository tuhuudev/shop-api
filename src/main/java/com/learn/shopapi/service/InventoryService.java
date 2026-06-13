package com.learn.shopapi.service;

import com.learn.shopapi.dto.InventoryMovementResponse;
import com.learn.shopapi.dto.PageResponse;
import com.learn.shopapi.repository.InventoryMovementRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Tra cuu nhat ky bien dong ton kho (doi soat). */
@Service
public class InventoryService {

    private final InventoryMovementRepository movementRepository;

    public InventoryService(InventoryMovementRepository movementRepository) {
        this.movementRepository = movementRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<InventoryMovementResponse> movements(Long productId, Pageable pageable) {
        var page = (productId != null)
                ? movementRepository.findByProductId(productId, pageable)
                : movementRepository.findAll(pageable);
        return PageResponse.from(page, InventoryMovementResponse::from);
    }
}
