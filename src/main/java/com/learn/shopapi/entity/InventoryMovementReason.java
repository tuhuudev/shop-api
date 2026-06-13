package com.learn.shopapi.entity;

/** Ly do bien dong ton kho. */
public enum InventoryMovementReason {
    ORDER_OUT,   // xuat kho khi tao don (changeQty am)
    RESTOCK      // nhap lai kho khi huy/hoan don (changeQty duong)
}
