package com.learn.shopapi.repository;

import com.learn.shopapi.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.util.List;

/**
 * Ngoai JpaRepository, ke thua them JpaSpecificationExecutor de loc DONG bang Specification
 * (ghep nhieu dieu kien tuy chon: ten + khoang gia + danh muc) ma khong de ra hang loat method.
 */
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    // Derived query: tim san pham theo ten gan dung (LIKE), khong phan biet hoa thuong.
    List<Product> findByNameContainingIgnoreCase(String keyword);

    // Derived query: san pham trong khoang gia.
    List<Product> findByPriceBetween(BigDecimal min, BigDecimal max);

    // San pham sap het hang.
    List<Product> findByStockQuantityLessThan(int threshold);

    // @Query voi JPQL (truy van theo Entity, khong phai ten bang SQL).
    @Query("SELECT p FROM Product p WHERE p.category.name = :categoryName")
    List<Product> findByCategoryName(@Param("categoryName") String categoryName);
}
