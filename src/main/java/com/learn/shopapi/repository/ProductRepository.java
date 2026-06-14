package com.learn.shopapi.repository;

import com.learn.shopapi.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
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
    // CANH BAO: "Containing" KHONG escape '%' / '_' -> tu khoa "50%" se khop moi dong.
    // Duong tim kiem that su dung ProductSpecifications.nameContains (da escape qua LikePatterns).
    // Neu can dung method nay, hay escape keyword bang LikePatterns truoc.
    List<Product> findByNameContainingIgnoreCase(String keyword);

    // Derived query: san pham trong khoang gia.
    List<Product> findByPriceBetween(BigDecimal min, BigDecimal max);

    // San pham sap het hang.
    List<Product> findByStockQuantityLessThan(int threshold);

    // @Query voi JPQL (truy van theo Entity, khong phai ten bang SQL).
    @Query("SELECT p FROM Product p WHERE p.category.name = :categoryName")
    List<Product> findByCategoryName(@Param("categoryName") String categoryName);

    /**
     * Tru ton kho ATOMIC tai DB: dieu kien "stock >= qty" nam ngay trong WHERE nen khong bao gio
     * ban am, va KHONG can optimistic-lock/retry khi nhieu don dat dong thoi cung san pham.
     * Tra ve so dong bi sua: 0 nghia la khong du kho. (San pham da soft-delete khong vao duoc
     * den day vi findById o OrderService da loc deleted=false truoc khi goi.)
     */
    @Modifying
    @Query("UPDATE Product p SET p.stockQuantity = p.stockQuantity - :qty "
            + "WHERE p.id = :id AND p.stockQuantity >= :qty")
    int decrementStock(@Param("id") Long id, @Param("qty") int qty);

    /** Hoan kho ATOMIC (khi huy don). */
    @Modifying
    @Query("UPDATE Product p SET p.stockQuantity = p.stockQuantity + :qty WHERE p.id = :id")
    int incrementStock(@Param("id") Long id, @Param("qty") int qty);
}
