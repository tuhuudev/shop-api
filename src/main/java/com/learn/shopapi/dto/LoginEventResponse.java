package com.learn.shopapi.dto;

import com.learn.shopapi.entity.LoginEvent;

import java.time.Instant;

/** Su kien dang nhap tra ve cho ADMIN xem. */
public record LoginEventResponse(Long id, String username, String outcome, String ip, Instant at) {
    public static LoginEventResponse from(LoginEvent e) {
        return new LoginEventResponse(e.getId(), e.getUsername(), e.getOutcome().name(), e.getIp(), e.getAt());
    }
}
