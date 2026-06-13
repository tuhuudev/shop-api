package com.learn.shopapi.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Bao boc ket qua phan trang de tra ve cho client gon gang, on dinh.
 *
 * Tai sao khong tra thang Page cua Spring? Vi cau truc JSON cua Page co the doi giua cac phien ban
 * va lo chi tiet noi bo. Tu dinh nghia DTO giup API "hop dong" ro rang (mau DTO pattern).
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last
) {
    /** Doi 1 Page<E> (entity) thanh PageResponse<T> (DTO) qua ham map. */
    public static <E, T> PageResponse<T> from(Page<E> page, Function<E, T> mapper) {
        List<T> content = page.getContent().stream().map(mapper).toList();
        return new PageResponse<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast());
    }
}
