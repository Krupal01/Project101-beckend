package com.crm.authentication.controller;

import com.crm.authentication.dto.AuthResponse;
import com.crm.authentication.dto.LoginRequest;
import com.crm.authentication.dto.RefreshTokenRequest;
import com.crm.authentication.dto.RegisterRequest;
import com.crm.authentication.service.AuthService;
import com.crm.authentication.service.TokenService;
import com.krunish.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final TokenService tokenService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<?>> register(@RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.ok(ApiResponse.success("User registered"));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @RequestBody RefreshTokenRequest request) {

        return ResponseEntity.ok(
                authService.refresh(request.getRefreshToken())
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @RequestBody RefreshTokenRequest request) {

        tokenService.revokeToken(request.getRefreshToken());
        return ResponseEntity.ok("Logged out successfully");
    }
}