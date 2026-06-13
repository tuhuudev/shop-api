package com.learn.shopapi.repository;

import com.learn.shopapi.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByProviderRef(String providerRef);
}
