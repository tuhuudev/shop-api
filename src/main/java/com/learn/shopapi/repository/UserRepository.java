package com.learn.shopapi.repository;

import com.learn.shopapi.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // Dung luc dang nhap: tim tai khoan theo username.
    Optional<User> findByUsername(String username);

    // Dung luc dang ky: kiem tra trung username/email truoc khi tao.
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
