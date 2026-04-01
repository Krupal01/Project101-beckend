package com.crm.authentication.entity;

import jakarta.persistence.*;
import lombok.Generated;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    private String phone;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private boolean isEmailVerified = false;

    @Column(nullable = false)
    private boolean isPhoneVerified = false;

    private String globalStatus = "ACTIVE"; // e.g., ACTIVE, INACTIVE, SUSPENDED
    private LocalDateTime lastLoginAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
