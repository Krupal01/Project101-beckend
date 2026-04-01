package com.crm.authentication.service;

import com.crm.authentication.configure.JwtConfig;
import com.crm.authentication.entity.RefreshToken;
import com.crm.authentication.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtConfig jwtConfig;
    private final PasswordEncoder passwordEncoder;

    public String createRefreshToken(Long userId) {

        String token = UUID.randomUUID().toString();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(userId);
        refreshToken.setTokenHash(passwordEncoder.encode(token));
        refreshToken.setExpiresAt(LocalDateTime.now()
                .plusSeconds(jwtConfig.getRefreshTokenExpiration() / 1000));
        refreshToken.setRevoked(false);

        RefreshToken saved = refreshTokenRepository.save(refreshToken);

        // Use auto-generated ID as the public lookup key
        return saved.getId() + ":" + token;
    }

    public RefreshToken validateRefreshToken(String token) {

        String[] parts = token.split(":", 2);
        if (parts.length != 2) {
            throw new RuntimeException("Invalid refresh token format");
        }

        String tokenId = parts[0];
        String secret  = parts[1];

        RefreshToken refreshToken = refreshTokenRepository.findById(Long.parseLong(tokenId))
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        // Verify the secret against stored hash
        if (!passwordEncoder.matches(secret, refreshToken.getTokenHash())) {
            throw new RuntimeException("Invalid refresh token");
        }

        if (refreshToken.isRevoked()) {
            throw new RuntimeException("Token revoked");
        }

        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Token expired");
        }

        return refreshToken;
    }

    public void revokeToken(String rawToken) {
        Long id = Long.parseLong(rawToken.split(":", 2)[0]);

        RefreshToken refreshToken = refreshTokenRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);
    }
}
