package com.learn.shopapi.repository;

import com.learn.shopapi.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {

    // Gio hang cua tai khoan dang dang nhap (Cart.user.username).
    Optional<Cart> findByUserUsername(String username);
}
