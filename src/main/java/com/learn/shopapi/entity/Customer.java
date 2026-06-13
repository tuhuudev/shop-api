package com.learn.shopapi.entity;

import com.learn.shopapi.common.Auditable;
import jakarta.persistence.*;

/**
 * Customer = ho so khach hang dat hang.
 *
 * Lien ket toi User (tai khoan dang nhap):
 * - Mot tai khoan CUSTOMER khi dang ky se duoc tao 1 Customer gan voi no.
 * - Nho vay khi dat don, ta lay Customer tu user dang dang nhap (khong tin customerId client gui).
 * - One-to-One, nullable: khach mua le do STAFF tao tay co the chua co tai khoan.
 */
@Entity
@Table(name = "customers")
public class Customer extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    protected Customer() { }

    public Customer(String name, String email) {
        this.name = name;
        this.email = email;
    }

    public Customer(String name, String email, User user) {
        this.name = name;
        this.email = email;
        this.user = user;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}
