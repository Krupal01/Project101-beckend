package com.crm.authentication.service;

import com.crm.authentication.dto.*;
import com.crm.authentication.entity.User;
import com.crm.authentication.exception.CustomException;
import com.crm.authentication.repository.UserRepository;
import com.crm.authentication.utils.JwtUtil;
import com.krunish.common.exception.BaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository  userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil         jwtUtil;
    private final TokenService    tokenService;

    /**
     * Feature flag — set auth.otp.enabled=false in application.properties to bypass OTP
     * and allow direct register/login without phone verification.
     * Default: true (OTP required).
     */
    @Value("${auth.otp.enabled:true}")
    private boolean otpEnabled;

    public UserExistResponse checkUserExistence(UserExistRequest request) {
        boolean exists = userRepository.existsByPhone(request.phone());
        return new UserExistResponse(exists);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Register
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Registers a new user.
     *
     * OTP ON  → otpToken (from /verify-otp) is required and validated.
     *           Phone is extracted from the token — not trusted from the request body.
     * OTP OFF → phone is taken directly from RegisterRequest; no token check.
     */
    public AuthResponse register(RegisterRequest request) {
        String verifiedPhone;

        if (otpEnabled) {
            // Extract phone from the signed otpToken — prevents registering arbitrary numbers
            verifiedPhone = jwtUtil.extractPhoneFromOtpToken(request.otpToken());

            // Cross-check that the phone in the request matches the verified token
            if (!verifiedPhone.equals(request.phone())) {
                throw new CustomException("Phone number does not match verified OTP session");
            }
        } else {
            // OTP disabled — trust the phone directly (use only in dev/internal environments)
            verifiedPhone = request.phone();
        }

        if (userRepository.existsByPhone(verifiedPhone)) {
            throw new BaseException("Phone number already registered",
                    HttpStatus.CONFLICT, "PHONE_ALREADY_EXISTS");
        }

        if (request.email() != null && !request.email().isBlank()
                && userRepository.existsByEmail(request.email())) {
            throw new BaseException("Email already registered",
                    HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS");
        }

        User user = new User();
        user.setPhone(verifiedPhone);
        user.setPhoneVerified(otpEnabled);        // true only if OTP was actually verified
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setGlobalStatus("ACTIVE");
        user.setRole("SEEKER");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        String accessToken  = jwtUtil.generateAccessToken(user);
        String refreshToken = tokenService.createRefreshToken(user.getId());
        return new AuthResponse(accessToken, refreshToken, false);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Login  (phone + password)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Direct login with phone + password.
     * LoginRequest uses phone (not email) — matches the entity's primary identifier.
     */
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByPhone(request.phone())
                .orElseThrow(() -> new CustomException("Invalid credentials"));

        if (user.getPasswordHash() == null) {
            throw new CustomException("No password set — please use OTP login");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new CustomException("Invalid credentials");
        }

        if ("SUSPENDED".equals(user.getGlobalStatus()) ||
                "BANNED".equals(user.getGlobalStatus())) {
            throw new CustomException("Account is " + user.getGlobalStatus().toLowerCase());
        }

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        String accessToken  = jwtUtil.generateAccessToken(user);
        String refreshToken = tokenService.createRefreshToken(user.getId());
        return new AuthResponse(accessToken, refreshToken, false);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Social Auth
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Google Sign-In.
     * Entity field is googleUid (not googleId) — must match exactly.
     */
    public AuthResponse loginWithGoogle(GoogleAuthRequest request) {
        GoogleTokenPayload payload = jwtUtil.verifyGoogleToken(request.idToken());

        User user = userRepository.findByGoogleUid(payload.googleUid()).orElseGet(() -> {
            // Check if the email is already registered via another method
            return userRepository.findByEmail(payload.email()).map(existing -> {
                // Link Google UID to the existing account
                existing.setGoogleUid(payload.googleUid());
                existing.setUpdatedAt(LocalDateTime.now());
                return userRepository.save(existing);
            }).orElseGet(() -> {
                User newUser = new User();
                newUser.setEmail(payload.email());
                newUser.setGoogleUid(payload.googleUid());
                newUser.setEmailVerified(true);       // Google emails are pre-verified
                newUser.setGlobalStatus("ACTIVE");
                newUser.setRole("SEEKER");
                newUser.setCreatedAt(LocalDateTime.now());
                newUser.setUpdatedAt(LocalDateTime.now());
                return userRepository.save(newUser);
            });
        });

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        String accessToken  = jwtUtil.generateAccessToken(user);
        String refreshToken = tokenService.createRefreshToken(user.getId());
        return new AuthResponse(accessToken, refreshToken, false);
    }

    /**
     * Apple Sign-In.
     * Entity field is appleUid (not appleId) — must match exactly.
     * fullName is only sent by Apple on the very first sign-in.
     */
    public AuthResponse loginWithApple(AppleAuthRequest request) {
        AppleTokenPayload payload = jwtUtil.verifyAppleToken(request.idToken());

        User user = userRepository.findByAppleUid(payload.appleUid()).orElseGet(() -> {
            return userRepository.findByEmail(payload.email()).map(existing -> {
                existing.setAppleUid(payload.appleUid());
                existing.setUpdatedAt(LocalDateTime.now());
                return userRepository.save(existing);
            }).orElseGet(() -> {
                User newUser = new User();
                newUser.setAppleUid(payload.appleUid());
                newUser.setEmail(payload.email());     // null on repeat sign-ins — that's fine
                newUser.setEmailVerified(payload.email() != null);
                newUser.setGlobalStatus("ACTIVE");
                newUser.setRole("SEEKER");
                newUser.setCreatedAt(LocalDateTime.now());
                newUser.setUpdatedAt(LocalDateTime.now());
                return userRepository.save(newUser);
            });
        });

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        String accessToken  = jwtUtil.generateAccessToken(user);
        String refreshToken = tokenService.createRefreshToken(user.getId());
        return new AuthResponse(accessToken, refreshToken, false);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Password Management
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Sets a password for users who registered via OTP and have no password yet.
     * User ID is resolved from the Security context (JWT subject).
     */
    public void setPassword(SetPasswordRequest request) {

        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new CustomException("User not found"));

        if (user.getPasswordHash() != null) {
            throw new BaseException("Password already set — use /password/change instead",
                    HttpStatus.CONFLICT, "PASSWORD_ALREADY_SET");
        }

        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    /**
     * Changes the authenticated user's password.
     * Revokes ALL refresh tokens to force re-login on all devices.
     */
    public void changePassword(ChangePasswordRequest request) {

        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new CustomException("User not found"));

        if (user.getPasswordHash() == null ||
                !passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new CustomException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        tokenService.revokeAllTokensForUser(user.getId());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internal helper
    // ─────────────────────────────────────────────────────────────────────────

    private UUID resolveCurrentUserId() {
        String subject = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        try {
            return UUID.fromString(subject);
        } catch (IllegalArgumentException e) {
            throw new CustomException("Invalid authentication context");
        }
    }
}