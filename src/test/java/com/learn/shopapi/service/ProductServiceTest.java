package com.learn.shopapi.service;

import com.learn.shopapi.dto.ProductRequest;
import com.learn.shopapi.dto.ProductResponse;
import com.learn.shopapi.entity.Category;
import com.learn.shopapi.entity.Product;
import com.learn.shopapi.exception.ResourceNotFoundException;
import com.learn.shopapi.repository.CategoryRepository;
import com.learn.shopapi.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Unit test cho ProductService (Mockito, khong dung DB that). */
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private CategoryRepository categoryRepository;

    @InjectMocks private ProductService productService;

    @Test
    void create_ganDanhMucVaLuu() {
        Category phones = new Category("Dien thoai");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(phones));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductRequest req = new ProductRequest("iPhone", "desc", new BigDecimal("1000"), 5, null, null, 1L);
        ProductResponse res = productService.create(req);

        assertThat(res.name()).isEqualTo("iPhone");
        assertThat(res.categoryName()).isEqualTo("Dien thoai");
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void findById_khongCo_nemResourceNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void delete_goiRepositoryDelete() {
        Product p = new Product("X", "d", new BigDecimal("1"), 1, null);
        when(productRepository.findById(1L)).thenReturn(Optional.of(p));

        productService.delete(1L);

        verify(productRepository).delete(p);   // soft delete xu ly o tang DB (@SQLDelete)
    }
}
