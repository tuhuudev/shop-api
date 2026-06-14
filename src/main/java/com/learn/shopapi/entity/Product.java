package com.learn.shopapi.entity;

import com.learn.shopapi.common.Auditable;
import jakarta.persistence.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;

/**
 * Product = mot san pham co the ban.
 * - Nhieu Product thuoc 1 Category  -> Many-to-One.
 * - Dung BigDecimal cho tien (KHONG dung double vi double lam tron sai so tien).
 * - Ke thua Auditable: tu co created_at/created_by/updated_at/updated_by.
 *
 * SOFT DELETE (xoa mem):
 * - @SQLDelete: khi goi delete(), Hibernate KHONG xoa that ma chay UPDATE set deleted=true.
 * - @SQLRestriction: moi cau SELECT do JPA sinh ra tu dong them "WHERE deleted = false",
 *   nen san pham da "xoa mem" se khong con xuat hien. Du lieu van con trong DB de truy vet.
 */
@Entity
@Table(name = "products", indexes = @Index(name = "idx_products_category", columnList = "category_id"))
@SQLDelete(sql = "UPDATE products SET deleted = true WHERE id = ?")
@SQLRestriction("deleted = false")
// Batch loading: khi nap nhieu OrderItem.product LAZY cung luc, Hibernate gom thanh vai cau IN
// thay vi N query (chong N+1 o list don hang).
@BatchSize(size = 100)
public class Product extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private int stockQuantity;

    // Anh dai dien (URL CDN ngoai). brand + rating phuc vu hien thi/loc o storefront.
    @Column(name = "image_url", length = 512)
    private String imageUrl;

    @Column(length = 128)
    private String brand;

    @Column(precision = 3, scale = 2)
    private BigDecimal rating;

    // FetchType.LAZY: chi tai Category khi thuc su can (toi uu hieu nang).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    // Co xoa mem: false = con ban, true = da "xoa". Xem @SQLDelete/@SQLRestriction o tren.
    @Column(nullable = false)
    private boolean deleted = false;

    // OPTIMISTIC LOCKING: moi lan UPDATE, Hibernate kiem tra version chua doi roi tang len.
    // Neu 2 giao dich cung sua 1 san pham, cai commit sau se that bai (OptimisticLockException)
    // -> tranh "ban am kho" khi nhieu don dat dong thoi.
    @Version
    private Long version;

    protected Product() { }

    public Product(String name, String description, BigDecimal price, int stockQuantity, Category category) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.category = category;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public int getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(int stockQuantity) { this.stockQuantity = stockQuantity; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public BigDecimal getRating() { return rating; }
    public void setRating(BigDecimal rating) { this.rating = rating; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
    public Long getVersion() { return version; }
}
