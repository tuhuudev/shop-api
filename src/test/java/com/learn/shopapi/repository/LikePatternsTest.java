package com.learn.shopapi.repository;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Unit test cho LikePatterns.escape (thuan logic, khong dung DB). */
class LikePatternsTest {

    @Test
    void escape_nullGiuNguyenNull() {
        assertThat(LikePatterns.escape(null)).isNull();
    }

    @Test
    void escape_chuoiThuong_khongDoi() {
        assertThat(LikePatterns.escape("iphone")).isEqualTo("iphone");
    }

    @Test
    void escape_phanTram() {
        assertThat(LikePatterns.escape("50%")).isEqualTo("50\\%");
    }

    @Test
    void escape_gachDuoi() {
        assertThat(LikePatterns.escape("a_b")).isEqualTo("a\\_b");
    }

    @Test
    void escape_backslash_escapeTruoc() {
        // '\' phai duoc nhan doi TRUOC khi escape '%' / '_', neu khong se sinh ra escape sai.
        assertThat(LikePatterns.escape("a\\%")).isEqualTo("a\\\\\\%");
    }

    @Test
    void escape_honHopNhieuKyTu() {
        assertThat(LikePatterns.escape("50%_x")).isEqualTo("50\\%\\_x");
    }
}
