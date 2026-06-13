package com.learn.shopapi.entity;

import com.learn.shopapi.common.Auditable;
import jakarta.persistence.*;

/**
 * Review = danh gia cua mot nguoi dung cho mot san pham (sao 1-5 + nhan xet).
 * Ke thua Auditable -> tu co createdAt/createdBy (ai danh gia, luc nao).
 */
@Entity
@Table(name = "reviews")
public class Review extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private int rating;   // 1..5

    @Column(length = 1000)
    private String comment;

    protected Review() { }

    public Review(Product product, User user, int rating, String comment) {
        this.product = product;
        this.user = user;
        this.rating = rating;
        this.comment = comment;
    }

    public Long getId() { return id; }
    public Product getProduct() { return product; }
    public User getUser() { return user; }
    public int getRating() { return rating; }
    public String getComment() { return comment; }
}
