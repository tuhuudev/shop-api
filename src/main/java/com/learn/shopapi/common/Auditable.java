package com.learn.shopapi.common;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Lop "cha" cho cac entity can ghi vet audit (ai tao, tao luc nao, sua lan cuoi).
 *
 * - @MappedSuperclass: KHONG phai 1 bang rieng. Cac cot ben duoi se duoc "tron"
 *   vao bang cua entity con (vi du bang products se co them created_at, created_by...).
 * - @EntityListeners(AuditingEntityListener): bat Spring Data tu dong dien gia tri
 *   vao 4 cot ben duoi moi khi INSERT/UPDATE (day la pattern Observer/Listener).
 *   De hoat dong, can @EnableJpaAuditing + 1 AuditorAware (xem JpaAuditingConfig).
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class Auditable {

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    // Username cua nguoi tao (lay tu SecurityContext qua AuditorAware).
    @CreatedBy
    @Column(updatable = false)
    private String createdBy;

    @LastModifiedBy
    private String updatedBy;

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public String getCreatedBy() { return createdBy; }
    public String getUpdatedBy() { return updatedBy; }
}
