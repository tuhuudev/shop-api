package com.learn.shopapi.entity;

import jakarta.persistence.*;

/**
 * Permission = mot QUYEN HANH chi tiet, vi du "PRODUCT_WRITE", "USER_MANAGE".
 *
 * Mo hinh phan quyen cua ta: User --co nhieu--> Role --gom nhieu--> Permission.
 * Khi dang nhap, ta gom tat ca permission cua cac role lai lam "authority" cho Spring Security.
 * Nho vay co the kiem soat rat min: 1 endpoint yeu cau dung 1 permission cu the.
 */
@Entity
@Table(name = "permissions")
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    protected Permission() { }

    public Permission(String name) {
        this.name = name;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
