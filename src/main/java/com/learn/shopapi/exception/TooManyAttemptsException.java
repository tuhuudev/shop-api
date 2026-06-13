package com.learn.shopapi.exception;

/**
 * Nem ra khi 1 tai khoan dang nhap sai qua nhieu lan -> tam khoa (chong do mat khau).
 * GlobalExceptionHandler doi thanh HTTP 429 Too Many Requests.
 */
public class TooManyAttemptsException extends RuntimeException {
    public TooManyAttemptsException(String message) {
        super(message);
    }
}
