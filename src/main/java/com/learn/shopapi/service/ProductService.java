package com.learn.shopapi.service;

import com.learn.shopapi.dto.PageResponse;
import com.learn.shopapi.dto.ProductRequest;
import com.learn.shopapi.dto.ProductResponse;
import com.learn.shopapi.entity.Category;
import com.learn.shopapi.entity.Product;
import com.learn.shopapi.exception.ResourceNotFoundException;
import com.learn.shopapi.repository.CategoryRepository;
import com.learn.shopapi.repository.ProductRepository;
import com.learn.shopapi.repository.ProductSpecifications;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Service = noi chua logic nghiep vu, dung giua Controller va Repository.
 * Controller chi nhan/tra HTTP; Service moi quyet dinh "lam gi voi du lieu".
 */
@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    // Spring TU DONG truyen (inject) cac repository vao day qua constructor.
    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    /**
     * Danh sach san pham CO PHAN TRANG + LOC DONG.
     * Cac tham so loc deu tuy chon (null = bo qua). Ghep dieu kien bang Specification.
     * Pageable do Spring tu tao tu ?page=&size=&sort= tren URL.
     */
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> find(String search, BigDecimal minPrice,
                                              BigDecimal maxPrice, Long categoryId, Pageable pageable) {
        Specification<Product> spec = ProductSpecifications.nameContains(search)
                .and(ProductSpecifications.priceGreaterOrEqual(minPrice))
                .and(ProductSpecifications.priceLessOrEqual(maxPrice))
                .and(ProductSpecifications.hasCategory(categoryId));
        return PageResponse.from(productRepository.findAll(spec, pageable), ProductResponse::from);
    }

    @Transactional(readOnly = true)
    public ProductResponse findById(Long id) {
        Product product = getProductOrThrow(id);
        return ProductResponse.from(product);
    }

    @Transactional
    public ProductResponse create(ProductRequest req) {
        Category category = resolveCategory(req.categoryId());
        Product product = new Product(
                req.name(), req.description(), req.price(), req.stockQuantity(), category);
        product.setImageUrl(req.imageUrl());
        product.setBrand(req.brand());
        return ProductResponse.from(productRepository.save(product));
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest req) {
        Product product = getProductOrThrow(id);
        product.setName(req.name());
        product.setDescription(req.description());
        product.setPrice(req.price());
        product.setStockQuantity(req.stockQuantity());
        product.setImageUrl(req.imageUrl());
        product.setBrand(req.brand());
        product.setCategory(resolveCategory(req.categoryId()));
        // Khong can goi save(): trong @Transactional, JPA tu phat hien thay doi (dirty checking).
        return ProductResponse.from(product);
    }

    @Transactional
    public void delete(Long id) {
        Product product = getProductOrThrow(id);
        productRepository.delete(product);
    }

    // ---- helper dung chung ----

    private Product getProductOrThrow(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay san pham id=" + id));
    }

    private Category resolveCategory(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay category id=" + categoryId));
    }
}
