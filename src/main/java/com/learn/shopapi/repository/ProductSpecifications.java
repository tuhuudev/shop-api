package com.learn.shopapi.repository;

import com.learn.shopapi.entity.Product;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

/**
 * Tap hop cac "manh" dieu kien loc san pham (mau Specification pattern).
 *
 * Y tuong: moi tieu chi la 1 Specification nho. Service ghep chung lai bang .and(...)
 * tuy theo client co truyen tham so do hay khong. Tranh phai viet rat nhieu method kieu
 * findByNameAndPriceBetweenAndCategory... cho moi to hop.
 *
 * Khi tham so vang mat, ta tra ve cb.conjunction() (dieu kien "luon dung") thay vi null,
 * de chuoi .and(...) luon an toan.
 */
public final class ProductSpecifications {

    private ProductSpecifications() { }

    /** Ten chua tu khoa (khong phan biet hoa thuong). */
    public static Specification<Product> nameContains(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) return cb.conjunction();
            return cb.like(cb.lower(root.get("name")), "%" + keyword.toLowerCase() + "%");
        };
    }

    /** Gia >= min. */
    public static Specification<Product> priceGreaterOrEqual(BigDecimal min) {
        return (root, query, cb) ->
                min == null ? cb.conjunction() : cb.greaterThanOrEqualTo(root.get("price"), min);
    }

    /** Gia <= max. */
    public static Specification<Product> priceLessOrEqual(BigDecimal max) {
        return (root, query, cb) ->
                max == null ? cb.conjunction() : cb.lessThanOrEqualTo(root.get("price"), max);
    }

    /** Thuoc danh muc co id cho truoc. */
    public static Specification<Product> hasCategory(Long categoryId) {
        return (root, query, cb) ->
                categoryId == null ? cb.conjunction() : cb.equal(root.get("category").get("id"), categoryId);
    }
}
