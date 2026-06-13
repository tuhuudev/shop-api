package com.learn.shopapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Dia chi giao hang nhap luc checkout. Toan bo doi tuong la TUY CHON tren don;
 * nhung neu da gui thi cac truong bat buoc duoi phai co (nguoi nhan/sdt/dia chi/thanh pho).
 */
public record ShippingAddressRequest(
        @NotBlank(message = "Ten nguoi nhan bat buoc") @Size(max = 255) String recipient,
        @NotBlank(message = "So dien thoai bat buoc") @Size(max = 32) String phone,
        @NotBlank(message = "Dia chi bat buoc") @Size(max = 255) String line1,
        @Size(max = 255) String line2,
        @NotBlank(message = "Thanh pho bat buoc") @Size(max = 128) String city,
        @Size(max = 128) String province,
        @Size(max = 16) String postalCode,
        @Size(max = 64) String country
) { }
