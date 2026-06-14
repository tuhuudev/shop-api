package com.learn.shopapi.exception;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Bat loi cho TOAN BO controller o mot cho duy nhat -> tra ve theo chuan RFC 7807
 * (ProblemDetail, content-type application/problem+json). Nho vay client xu ly loi nhat quan.
 *
 * Ke thua ResponseEntityExceptionHandler de cac exception khung Spring MVC (sai HTTP method ->405,
 * Content-Type khong ho tro ->415, type-mismatch/thieu param ->400, khong tim thay route ->404...)
 * duoc map dung ma 4xx TRUOC khi roi vao luoi cuoi Exception->500. Cac template method
 * handleHttpMessageNotReadable/handleMethodArgumentNotValid duoc override de giu title/format tieng Viet.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Khong tim thay -> 404
    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Khong tim thay", ex.getMessage());
    }

    // Vi pham nghiep vu (vi du het hang) -> 400
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleBadRequest(IllegalArgumentException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Yeu cau khong hop le", ex.getMessage());
    }

    // Sai username/mat khau hoac refresh token khong hop le -> 401
    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuth(AuthenticationException ex) {
        return problem(HttpStatus.UNAUTHORIZED, "Xac thuc that bai", ex.getMessage());
    }

    // Dang nhap sai qua nhieu lan -> 429 Too Many Requests
    @ExceptionHandler(TooManyAttemptsException.class)
    public ProblemDetail handleTooMany(TooManyAttemptsException ex) {
        return problem(HttpStatus.TOO_MANY_REQUESTS, "Qua nhieu yeu cau", ex.getMessage());
    }

    // Du phong: neu RefreshTokenReuseException loi ra ngoai (binh thuong da xu ly o AuthService) -> 401
    @ExceptionHandler(RefreshTokenReuseException.class)
    public ProblemDetail handleReuse(RefreshTokenReuseException ex) {
        return problem(HttpStatus.UNAUTHORIZED, "Xac thuc that bai", ex.getMessage());
    }

    // Xung dot ghi dong thoi (optimistic locking) -> 409 Conflict. Client co the thu lai.
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
        return problem(HttpStatus.CONFLICT, "Xung dot du lieu",
                "Du lieu vua bi thay doi boi mot thao tac khac, vui long thu lai");
    }

    // JSON gui len sai dinh dang / thieu field bat buoc -> 400 (override template cua superclass)
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail pd = problem(HttpStatus.BAD_REQUEST, "Body khong doc duoc",
                "Body JSON khong hop le hoac thieu truong bat buoc");
        return handleExceptionInternal(ex, pd, headers, HttpStatus.BAD_REQUEST, request);
    }

    // Validation that bai (@NotBlank, @Positive...) -> 400 kem chi tiet tung field (override template)
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> fieldErrors.put(e.getField(), e.getDefaultMessage()));
        ProblemDetail pd = problem(HttpStatus.BAD_REQUEST, "Du lieu khong hop le",
                "Mot so truong khong dat yeu cau");
        pd.setProperty("errors", fieldErrors);
        return handleExceptionInternal(ex, pd, headers, HttpStatus.BAD_REQUEST, request);
    }

    // Validation o tang service/Hibernate (@Validated, constraint tren entity) -> 400 kem tung field
    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getConstraintViolations().forEach(v -> {
            String path = v.getPropertyPath().toString();
            String field = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
            fieldErrors.put(field, v.getMessage());
        });
        ProblemDetail pd = problem(HttpStatus.BAD_REQUEST, "Du lieu khong hop le",
                "Mot so truong khong dat yeu cau");
        pd.setProperty("errors", fieldErrors);
        return pd;
    }

    // Vi pham rang buoc toan ven DB (unique/FK/not-null) -> 409. KHONG leak SQL/ten cot (lo schema).
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation", ex);
        return problem(HttpStatus.CONFLICT, "Xung dot du lieu",
                "Du lieu vi pham rang buoc toan ven (trung lap hoac tham chieu khong hop le)");
    }

    // Rethrow de ExceptionTranslationFilter/RestAccessDeniedHandler giu nguyen 403 (khong roi vao fallback).
    @ExceptionHandler(AccessDeniedException.class)
    public void handleAccessDenied(AccessDeniedException ex) {
        throw ex;
    }

    // Luoi cuoi: loi chua co handler rieng -> 500 generic. Giu chi tiet o log server, KHONG tra ve client.
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleFallback(Exception ex) {
        log.error("Unhandled exception", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Loi he thong",
                "Da co loi xay ra, vui long thu lai sau");
    }

    /** Tao ProblemDetail chuan, them moc thoi gian. */
    private ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(title);
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }
}
