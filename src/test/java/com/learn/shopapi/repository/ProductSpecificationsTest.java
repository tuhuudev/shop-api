package com.learn.shopapi.repository;

import com.learn.shopapi.entity.Product;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyChar;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Unit test cho ProductSpecifications (mock CriteriaBuilder, khong dung DB). */
@ExtendWith(MockitoExtension.class)
class ProductSpecificationsTest {

    @Mock private Root<Product> root;
    @Mock private CriteriaQuery<?> query;
    @Mock private CriteriaBuilder cb;

    @Test
    void nameContains_blank_traVeConjunction_khongGoiLike() {
        Predicate conjunction = mock(Predicate.class);
        when(cb.conjunction()).thenReturn(conjunction);

        Predicate result = ProductSpecifications.nameContains("   ").toPredicate(root, query, cb);

        assertThat(result).isSameAs(conjunction);
        verify(cb, never()).like(any(Expression.class), anyString(), anyChar());
    }

    @SuppressWarnings("unchecked")
    @Test
    void nameContains_escapeWildcard_LIKE() {
        Path<String> namePath = mock(Path.class);
        Expression<String> lowered = mock(Expression.class);
        doReturn(namePath).when(root).get("name");
        when(cb.lower(namePath)).thenReturn(lowered);

        ProductSpecifications.nameContains("50%_x").toPredicate(root, query, cb);

        ArgumentCaptor<String> patternCap = ArgumentCaptor.forClass(String.class);
        // '%' va '_' phai bi escape thanh "\%" / "\_" de coi la ky tu thuong; escape char = '\'.
        verify(cb).like(eq(lowered), patternCap.capture(), eq('\\'));
        assertThat(patternCap.getValue()).isEqualTo("%50\\%\\_x%");
    }
}
