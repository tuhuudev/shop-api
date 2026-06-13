package com.learn.shopapi.service;

import com.learn.shopapi.dto.CategoryRequest;
import com.learn.shopapi.dto.CategoryResponse;
import com.learn.shopapi.dto.PageResponse;
import com.learn.shopapi.entity.Category;
import com.learn.shopapi.exception.ResourceNotFoundException;
import com.learn.shopapi.repository.CategoryRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Quan ly danh muc. Danh muc it thay doi -> dung CACHE cho danh sach (xem @Cacheable),
 * va XOA cache (@CacheEvict) moi khi ghi de tranh tra du lieu cu.
 */
@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<CategoryResponse> findAll(Pageable pageable) {
        return PageResponse.from(categoryRepository.findAll(pageable), CategoryResponse::from);
    }

    /** Danh sach gon (khong phan trang) - duoc cache vi it doi va hay dung cho dropdown. */
    @Cacheable("categories")
    @Transactional(readOnly = true)
    public List<CategoryResponse> findAllCached() {
        return categoryRepository.findAll().stream().map(CategoryResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse findById(Long id) {
        return CategoryResponse.from(getOrThrow(id));
    }

    @CacheEvict(value = "categories", allEntries = true)
    @Transactional
    public CategoryResponse create(CategoryRequest req) {
        if (categoryRepository.findByName(req.name()).isPresent()) {
            throw new IllegalArgumentException("Danh muc da ton tai: " + req.name());
        }
        return CategoryResponse.from(categoryRepository.save(new Category(req.name())));
    }

    @CacheEvict(value = "categories", allEntries = true)
    @Transactional
    public CategoryResponse update(Long id, CategoryRequest req) {
        Category category = getOrThrow(id);
        category.setName(req.name());
        return CategoryResponse.from(category);
    }

    @CacheEvict(value = "categories", allEntries = true)
    @Transactional
    public void delete(Long id) {
        categoryRepository.delete(getOrThrow(id));
    }

    private Category getOrThrow(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay danh muc id=" + id));
    }
}
