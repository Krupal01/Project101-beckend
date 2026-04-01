package com.crm.authentication.entity;

import jakarta.persistence.*;
import lombok.Setter;
import lombok.Getter;

import java.time.LocalDateTime;

@Entity
@Table(name = "refresh_tokens")
@Getter @Setter
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    @Column(length = 500)
    private String tokenHash;
    private String deviceInfo;
    private String ipAddress;

    private LocalDateTime expiresAt;
    private boolean revoked = false;
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
