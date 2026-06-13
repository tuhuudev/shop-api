package com.learn.shopapi.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Nhat ky bien dong ton kho (append-only): moi lan kho tang/giam deu ghi 1 dong.
 * Phuc vu doi soat - tach khoi @Version (von chi chong ghi de dong thoi).
 *
 * changeQty: AM khi xuat (ban hang), DUONG khi nhap (hoan kho). reason cho biet ly do,
 * orderId tham chieu don lien quan (null neu dieu chinh thu cong).
 */
@Entity
@Table(name = "inventory_movements", indexes = {
        @Index(name = "idx_inv_mov_product", columnList = "product_id"),
        @Index(name = "idx_inv_mov_order", columnList = "order_id")
})
public class InventoryMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "change_qty", nullable = false)
    private int changeQty;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InventoryMovementReason reason;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", updatable = false)
    private String createdBy;

    protected InventoryMovement() { }

    public InventoryMovement(Long productId, int changeQty, InventoryMovementReason reason,
                             Long orderId, String createdBy) {
        this.productId = productId;
        this.changeQty = changeQty;
        this.reason = reason;
        this.orderId = orderId;
        this.createdBy = createdBy;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getProductId() { return productId; }
    public int getChangeQty() { return changeQty; }
    public InventoryMovementReason getReason() { return reason; }
    public Long getOrderId() { return orderId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getCreatedBy() { return createdBy; }
}
