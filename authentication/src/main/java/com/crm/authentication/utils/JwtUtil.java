package com.crm.authentication.utils;

import com.crm.authentication.configure.JwtConfig;
import com.crm.authentication.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final JwtConfig jwtConfig;

    public String generateAccessToken(User user) {

        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtConfig.getAccessTokenExpiration());

        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(jwtConfig.getSigningKey())
                .compact();
    }

    public Long extractUserId(String token) {

        Claims claims = Jwts.parser()
                .verifyWith((SecretKey) jwtConfig.getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return Long.parseLong(claims.getSubject());
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith((SecretKey) jwtConfig.getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }
}
