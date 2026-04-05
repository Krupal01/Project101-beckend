package com.crm.authentication.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true, length = 15)
    private String phone;

    @Column(unique = true, length = 150)
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "google_uid", unique = true)
    private String googleUid;

    @Column(name = "apple_uid", unique = true)
    private String appleUid;

    @Column(name = "phone_verified")
    private boolean phoneVerified = false;

    @Column(name = "email_verified")
    private boolean emailVerified = false;

    @Column(name = "kyc_status")
    private String kycStatus = "NONE";

    @Column(name = "global_status")
    private String globalStatus = "ACTIVE";

    @Column(name = "role")
    private String role = "SEEKER";

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}