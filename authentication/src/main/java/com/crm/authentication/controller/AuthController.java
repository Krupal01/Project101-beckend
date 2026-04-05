package com.crm.authentication.controller;

import com.crm.authentication.dto.*;
import com.crm.authentication.service.AuthService;
import com.crm.authentication.service.OtpService;
import com.crm.authentication.service.TokenService;
import com.krunish.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final OtpService otpService;
    private final AuthService authService;
    private final TokenService tokenService;

    // ─────────────────────────────────────────────────────────────────────────
    // OTP
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Step 1 of OTP flow.
     * Sends an OTP to the given phone number and returns a short-lived otpToken
     * that must be passed back in /verify-otp to bind the session.
     */
    @PostMapping("/send-otp")
    public ResponseEntity<ApiResponse<SendOtpResponse>> sendOtp(
            @Valid @RequestBody SendOtpRequest request) {

        SendOtpResponse response = otpService.sendOtp(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Step 2 of OTP flow.
     * Verifies the OTP against the stored hash.
     * - New user  → returns { otpToken (phone-verified proof), isNewUser: true }
     *               client must call /register next
     * - Old user  → returns { accessToken, refreshToken, isNewUser: false }
     *               login complete
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request) {

        AuthResponse response = otpService.verifyOtp(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Check User Existence (for frontend validation before registration), if no OTP.
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Step 1 of new-user flow (called only when isNewUser = true).
     * Validates the phone-verified otpToken from /verify-otp,
     * creates the user row, and returns auth tokens.
     */
    @PostMapping("/check-user-existence")
    public ResponseEntity<ApiResponse<UserExistResponse>> checkUserExist(
            @Valid @RequestBody UserExistRequest request) {

        UserExistResponse response = authService.checkUserExistence(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Register & Login
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Step 3 of new-user flow (called only when isNewUser = true).
     * Validates the phone-verified otpToken from /verify-otp,
     * creates the user row, and returns auth tokens.
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {

        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Direct login for returning users who have a password set.
     * Does NOT require a prior OTP step.
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Social Auth
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Google Sign-In.
     * Verifies the idToken with Google, creates user if first time,
     * logs in if returning.
     */
    @PostMapping("/social/google")
    public ResponseEntity<ApiResponse<AuthResponse>> googleLogin(
            @Valid @RequestBody GoogleAuthRequest request) {

        AuthResponse response = authService.loginWithGoogle(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Apple Sign-In.
     * fullName is only sent by Apple on first sign-in; ignored on subsequent calls.
     */
    @PostMapping("/social/apple")
    public ResponseEntity<ApiResponse<AuthResponse>> appleLogin(
            @Valid @RequestBody AppleAuthRequest request) {

        AuthResponse response = authService.loginWithApple(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Token Management
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Issues a new accessToken using a valid, non-revoked refreshToken.
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenRefreshResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request) {

        TokenRefreshResponse response = tokenService.refresh(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Revokes the given refreshToken (single device logout).
     * Requires a valid JWT in the Authorization header.
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Valid @RequestBody LogoutRequest request) {

        tokenService.revokeToken(request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Password Management  (JWT protected — handled by security filter)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Sets a password for users who registered via OTP and have no password yet.
     * Idempotent — calling again after a password is set should be rejected
     * at the service layer.
     */
    @PostMapping("/password/set")
    public ResponseEntity<ApiResponse<Void>> setPassword(
            @Valid @RequestBody SetPasswordRequest request) {

        authService.setPassword(request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * Changes the password for authenticated users.
     * Revokes ALL refresh tokens on success (forces re-login on all devices).
     */
    @PostMapping("/password/change")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {

        authService.changePassword(request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
