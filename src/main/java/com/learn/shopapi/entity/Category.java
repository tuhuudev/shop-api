package com.learn.shopapi.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Category = nhom san pham (vi du: "Dien thoai", "Sach").
 * 1 Category co NHIEU Product  ->  quan he One-to-Many.
 */
@Entity
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    // mappedBy = "category": phia Product giu khoa ngoai (cot category_id).
    // Day chi la danh sach nguoc lai de tien truy van.
    @OneToMany(mappedBy = "category")
    private List<Product> products = new ArrayList<>();

    protected Category() { } // JPA bat buoc co constructor rong

    public Category(String name) {
        this.name = name;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<Product> getProducts() { return products; }
}
