package com.learn.shopapi.exception;

/** Nem ra khi khong tim thay ban ghi (vi du product id khong ton tai). */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
