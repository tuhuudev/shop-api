package com.learn.shopapi.repository;

import com.learn.shopapi.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/**
 * Chi can ke thua JpaRepository<Entity, KieuKhoaChinh> la co san:
 * save(), findById(), findAll(), deleteById(), count()...
 */
public interface CategoryRepository extends JpaRepository<Category, Long> {

    // Spring TU sinh cau truy van tu TEN method: "find By Name".
    Optional<Category> findByName(String name);
}
