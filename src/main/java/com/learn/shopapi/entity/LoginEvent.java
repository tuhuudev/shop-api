package com.learn.shopapi.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Ghi vet moi lan dang nhap (thanh cong/that bai/bi chan) de phuc vu BAO MAT:
 * dieu tra su co, phat hien tan cong do mat khau. Luu DB (khac voi LoginAttemptService chi dem trong RAM).
 */
@Entity
@Table(name = "login_events", indexes = @Index(name = "idx_login_events_at", columnList = "at"))
public class LoginEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoginOutcome outcome;

    private String ip;

    @Column(nullable = false)
    private Instant at;

    protected LoginEvent() { }

    public LoginEvent(String username, LoginOutcome outcome, String ip) {
        this.username = username;
        this.outcome = outcome;
        this.ip = ip;
        this.at = Instant.now();
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public LoginOutcome getOutcome() { return outcome; }
    public String getIp() { return ip; }
    public Instant getAt() { return at; }
}
