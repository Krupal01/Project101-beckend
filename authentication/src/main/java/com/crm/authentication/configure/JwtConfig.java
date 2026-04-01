package com.crm.authentication.configure;

import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import javax.crypto.SecretKey;

@Configuration
@ConfigurationProperties(prefix = "jwt")
@Getter
@Setter
public class JwtConfig {

    private String secret;
    private long accessTokenExpiration;
    private long refreshTokenExpiration;

    public SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }
}