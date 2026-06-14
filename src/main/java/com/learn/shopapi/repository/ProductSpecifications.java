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

    /** Ky tu escape cho LIKE - de coi '%' va '_' trong tu khoa la ky tu thuong, khong phai wildcard. */
    private static final char LIKE_ESCAPE = '\\';

    /** Ten chua tu khoa (khong phan biet hoa thuong). */
    public static Specification<Product> nameContains(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return cb.conjunction();
            }
            // Escape wildcard LIKE ('\', '%', '_') de tu khoa "50%" khong khop moi san pham.
            // Thu tu quan trong: escape ky tu escape ('\') TRUOC, roi moi den '%' va '_'.
            String pattern = keyword.toLowerCase()
                    .replace("\\", "\\\\")
                    .replace("%", "\\%")
                    .replace("_", "\\_");
            return cb.like(cb.lower(root.get("name")), "%" + pattern + "%", LIKE_ESCAPE);
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
