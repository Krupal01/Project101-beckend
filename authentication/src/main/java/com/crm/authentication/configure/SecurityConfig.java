package com.crm.authentication.configure;

import com.krunish.common.security.AuthProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityConfig {

    private final AuthProperties authProperties;

    @PostConstruct
    public void debug() {
        System.out.println(">>> Public Paths: " + authProperties.getPublicPaths());
        System.out.println(">>> Secret: " + authProperties.getSecurity().getSecret());
    }
}