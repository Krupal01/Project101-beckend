package com.crm.authentication.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "phone_otps")
@Getter
@Setter
public class PhoneOtp {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 15)
    private String phone;

    @Column(name = "otp_hash", nullable = false)
    private String otpHash;

    @Column(nullable = false)
    private String purpose;

    private Integer attempts = 0;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}