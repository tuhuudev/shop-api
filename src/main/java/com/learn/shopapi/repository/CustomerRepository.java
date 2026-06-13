package com.learn.shopapi.repository;

import com.learn.shopapi.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByEmail(String email);

    // Tim ho so khach hang gan voi tai khoan dang nhap (Customer.user.username).
    Optional<Customer> findByUserUsername(String username);
}
