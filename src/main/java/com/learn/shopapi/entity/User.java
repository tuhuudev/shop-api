package com.learn.shopapi.entity;

import com.learn.shopapi.common.Auditable;
import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * User = TAI KHOAN dang nhap. Khac voi Customer (ho so khach hang mua hang).
 * 1 User (role CUSTOMER) co the gan voi 1 Customer (xem Customer.user).
 *
 * - password luu DANG BAM (BCrypt), KHONG bao gio luu mat khau goc.
 * - enabled: cho phep "khoa" tai khoan ma khong xoa.
 * - Ke thua Auditable: tu co created_at/created_by...
 */
@Entity
@Table(name = "users")
public class User extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    private String email;

    private String fullName;

    @Column(nullable = false)
    private boolean enabled = true;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();

    protected User() { }

    public User(String username, String password, String email, String fullName) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.fullName = fullName;
        this.enabled = true;
    }

    public void addRole(Role role) {
        roles.add(role);
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public Set<Role> getRoles() { return roles; }
    public void setRoles(Set<Role> roles) { this.roles = roles; }
}
