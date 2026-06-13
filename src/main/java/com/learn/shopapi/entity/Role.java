package com.learn.shopapi.entity;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Role = mot VAI TRO, vi du ADMIN, STAFF, CUSTOMER.
 *
 * - Ten luu khong kem tien to "ROLE_" (vi du "ADMIN"). Spring Security quy uoc
 *   hasRole("ADMIN") se kiem tra authority "ROLE_ADMIN" -> ta them tien to luc build authority.
 * - 1 Role gom nhieu Permission (Many-to-Many qua bang noi role_permissions).
 */
@Entity
@Table(name = "roles")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    // EAGER: khi nap Role thi nap luon Permission (vi luc dang nhap ta luon can chung).
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "role_permissions",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id"))
    private Set<Permission> permissions = new HashSet<>();

    protected Role() { }

    public Role(String name) {
        this.name = name;
    }

    public void addPermission(Permission permission) {
        permissions.add(permission);
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Set<Permission> getPermissions() { return permissions; }
    public void setPermissions(Set<Permission> permissions) { this.permissions = permissions; }
}
