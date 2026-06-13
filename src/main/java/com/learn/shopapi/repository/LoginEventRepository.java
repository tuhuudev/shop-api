package com.learn.shopapi.repository;

import com.learn.shopapi.entity.LoginEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginEventRepository extends JpaRepository<LoginEvent, Long> {

    Page<LoginEvent> findAllByOrderByAtDesc(Pageable pageable);
}
