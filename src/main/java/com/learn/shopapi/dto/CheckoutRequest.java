package com.learn.shopapi.dto;

import jakarta.validation.Valid;

/** Body (tuy chon) khi checkout gio hang: ma giam gia + dia chi giao. */
public record CheckoutRequest(
        String couponCode,
        @Valid ShippingAddressRequest shipping
) { }
