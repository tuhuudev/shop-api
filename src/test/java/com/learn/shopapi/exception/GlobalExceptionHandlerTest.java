package com.learn.shopapi.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit test thuan (khong Spring/DB) cho cac handler moi cua GlobalExceptionHandler:
 * 400 ConstraintViolation, 409 DataIntegrity (khong leak), 500 fallback (khong leak),
 * va rethrow AccessDeniedException (giu 403).
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    static class SampleBean {
        @NotBlank(message = "ten khong duoc trong")
        private final String name;

        SampleBean(String name) { this.name = name; }

        public String getName() { return name; }
    }

    @Test
    void constraintViolation_tra400VoiErrors() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        Set<ConstraintViolation<SampleBean>> violations = validator.validate(new SampleBean(""));

        ProblemDetail pd = handler.handleConstraintViolation(new ConstraintViolationException(violations));

        assertThat(pd.getStatus()).isEqualTo(400);
        assertThat(pd.getTitle()).isEqualTo("Du lieu khong hop le");
        @SuppressWarnings("unchecked")
        Map<String, String> errors = (Map<String, String>) pd.getProperties().get("errors");
        assertThat(errors).containsKey("name");
        assertThat(errors.get("name")).isEqualTo("ten khong duoc trong");
    }

    @Test
    void dataIntegrity_tra409KhongLeakChiTiet() {
        ProblemDetail pd = handler.handleDataIntegrity(new DataIntegrityViolationException(
                "ERROR: duplicate key value violates unique constraint \"uk_users_email\""));

        assertThat(pd.getStatus()).isEqualTo(409);
        assertThat(pd.getTitle()).isEqualTo("Xung dot du lieu");
        assertThat(pd.getDetail())
                .doesNotContain("uk_users_email")
                .doesNotContain("unique constraint")
                .doesNotContain("duplicate key");
    }

    @Test
    void fallback_tra500Generic() {
        ProblemDetail pd = handler.handleFallback(new RuntimeException("chi tiet noi bo nhay cam"));

        assertThat(pd.getStatus()).isEqualTo(500);
        assertThat(pd.getTitle()).isEqualTo("Loi he thong");
        assertThat(pd.getDetail()).isEqualTo("Da co loi xay ra, vui long thu lai sau");
        assertThat(pd.getDetail()).doesNotContain("chi tiet noi bo nhay cam");
    }

    @Test
    void accessDenied_duocRethrow() {
        AccessDeniedException ex = new AccessDeniedException("forbidden");
        assertThatThrownBy(() -> handler.handleAccessDenied(ex)).isSameAs(ex);
    }
}
