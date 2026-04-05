package com.crm.authentication.service;

import com.crm.authentication.configure.JwtConfig;
import com.crm.authentication.dto.LogoutRequest;
import com.crm.authentication.dto.RefreshTokenRequest;
import com.crm.authentication.dto.TokenRefreshResponse;
import com.crm.authentication.entity.RefreshToken;
import com.crm.authentication.entity.User;
import com.crm.authentication.exception.CustomException;
import com.crm.authentication.repository.RefreshTokenRepository;
import com.crm.authentication.repository.UserRepository;
import com.crm.authentication.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository         userRepository;
    private final JwtConfig              jwtConfig;
    private final JwtUtil                jwtUtil;
    private final PasswordEncoder        passwordEncoder;

    // ─────────────────────────────────────────────────────────────────────────
    // Create
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Generates a refresh token for the given user.
     * Stores a bcrypt hash; returns "{uuid}:{secret}" to the caller.
     */
    public String createRefreshToken(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException("User not found"));

        String secret = UUID.randomUUID().toString();

        RefreshToken token = new RefreshToken();
        token.setUser(user);                                        // @ManyToOne — set the entity, not a field
        token.setTokenHash(passwordEncoder.encode(secret));
        token.setExpiresAt(LocalDateTime.now()
                .plusSeconds(jwtConfig.getRefreshTokenExpiration() / 1000));
        token.setIsRevoked(false);
        token.setCreatedAt(LocalDateTime.now());

        RefreshToken saved = refreshTokenRepository.save(token);
        return saved.getId() + ":" + secret;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Refresh
    // ─────────────────────────────────────────────────────────────────────────

    public TokenRefreshResponse refresh(RefreshTokenRequest request) {
        RefreshToken stored = validateRefreshToken(request.refreshToken());

        // User already hydrated by JPA via @ManyToOne — no extra query
        User user = stored.getUser();

        // Update lastLoginAt on every token refresh
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        String newAccessToken = jwtUtil.generateAccessToken(user);
        long expiresIn = jwtConfig.getAccessTokenExpiration() / 1000; // ms → seconds
        return new TokenRefreshResponse(newAccessToken, expiresIn);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Revoke
    // ─────────────────────────────────────────────────────────────────────────

    /** Single-device logout — revokes one token. */
    public void revokeToken(LogoutRequest request) {
        revokeByRawValue(request.refreshToken());
    }

    /** All-device logout — revokes every token for a user. Called after password change. */
    @Transactional
    public void revokeAllTokensForUser(UUID userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internal helpers
    // ─────────────────────────────────────────────────────────────────────────

    public RefreshToken validateRefreshToken(String rawToken) {
        String[] parts = rawToken.split(":", 2);
        if (parts.length != 2) {
            throw new CustomException("Invalid refresh token format");
        }

        UUID tokenId;
        try {
            tokenId = UUID.fromString(parts[0]);
        } catch (IllegalArgumentException e) {
            throw new CustomException("Invalid refresh token format");
        }

        String secret = parts[1];

        RefreshToken refreshToken = refreshTokenRepository.findById(tokenId)
                .orElseThrow(() -> new CustomException("Invalid refresh token"));

        if (!passwordEncoder.matches(secret, refreshToken.getTokenHash())) {
            throw new CustomException("Invalid refresh token");
        }

        // Boolean wrapper — null-safe check before unboxing
        if (Boolean.TRUE.equals(refreshToken.getIsRevoked())) {
            throw new CustomException("Token has been revoked");
        }

        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new CustomException("Token has expired");
        }

        return refreshToken;
    }

    private void revokeByRawValue(String rawToken) {
        String[] parts = rawToken.split(":", 2);
        if (parts.length != 2) {
            throw new CustomException("Invalid refresh token format");
        }

        UUID tokenId;
        try {
            tokenId = UUID.fromString(parts[0]);
        } catch (IllegalArgumentException e) {
            throw new CustomException("Invalid refresh token format");
        }

        RefreshToken refreshToken = refreshTokenRepository.findById(tokenId)
                .orElseThrow(() -> new CustomException("Invalid refresh token"));

        refreshToken.setIsRevoked(true);
        refreshTokenRepository.save(refreshToken);
    }
}