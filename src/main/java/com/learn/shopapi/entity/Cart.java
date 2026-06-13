package com.learn.shopapi.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Cart = gio hang cua MOT tai khoan (1-1 voi User).
 * Chua cac CartItem; khi checkout se bien thanh Order roi duoc xoa rong.
 */
@Entity
@Table(name = "carts")
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> items = new ArrayList<>();

    protected Cart() { }

    public Cart(User user) {
        this.user = user;
    }

    public CartItem findItem(Long productId) {
        return items.stream()
                .filter(i -> i.getProduct().getId().equals(productId))
                .findFirst().orElse(null);
    }

    public void addItem(CartItem item) {
        items.add(item);
        item.setCart(this);
    }

    public void removeItem(CartItem item) {
        items.remove(item);
        item.setCart(null);
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public List<CartItem> getItems() { return items; }
}
