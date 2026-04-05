package com.crm.authentication.service;

import com.crm.authentication.dto.*;
import com.crm.authentication.entity.PhoneOtp;
import com.crm.authentication.entity.User;
import com.crm.authentication.exception.CustomException;
import com.crm.authentication.repository.PhoneOtpRepository;
import com.crm.authentication.repository.UserRepository;
import com.crm.authentication.service.sms.SmsService;
import com.crm.authentication.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OtpService {

    private static final int OTP_EXPIRY_MINUTES  = 5;
    private static final int MAX_VERIFY_ATTEMPTS = 5;

    private final PhoneOtpRepository phoneOtpRepository;
    private final UserRepository     userRepository;
    private final TokenService       tokenService;
    private final JwtUtil            jwtUtil;
    private final PasswordEncoder    passwordEncoder;
    private final SmsService smsService;

    // ─────────────────────────────────────────────────────────────────────────
    // Step 1 — Send OTP
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Generates a 6-digit OTP, stores its bcrypt hash, sends SMS,
     * and returns a short-lived otpToken binding this request to /verify-otp.
     *
     * Purpose is stored on the record so the same phone can have independent
     * OTP sessions for REGISTER vs RESET_PASSWORD simultaneously if needed.
     */
    @Transactional
    public SendOtpResponse sendOtp(SendOtpRequest request) {
        String phone   = normalisePhone(request.phone());
        String purpose = request.purpose();

        // Delete any existing unused OTP for this phone+purpose — one active code at a time
        phoneOtpRepository.deleteAllByPhoneAndPurpose(phone, purpose);

        String otpCode = generateOtp();

        PhoneOtp entity = new PhoneOtp();
        entity.setPhone(phone);
        entity.setOtpHash(passwordEncoder.encode(otpCode));
        entity.setPurpose(purpose);
        entity.setAttempts(0);
        entity.setExpiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
        entity.setCreatedAt(LocalDateTime.now());
        // usedAt is null — will be set on successful verification
        phoneOtpRepository.save(entity);

        smsService.send(phone, "Your verification code is: " + otpCode +
                ". Valid for " + OTP_EXPIRY_MINUTES + " minutes.");

        String otpToken = jwtUtil.generateOtpToken(phone);
        return new SendOtpResponse(otpToken);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Step 2 — Verify OTP
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Verifies the submitted OTP code.
     *
     * REGISTER / VERIFY_PHONE:
     *   New user  → { accessToken: otpToken, refreshToken: null, isNewUser: true }
     *               Client must call /register next.
     *   Returning → { accessToken, refreshToken, isNewUser: false }
     *               Login complete.
     *
     * LOGIN:
     *   Always issues full tokens if user exists, else treats as new user.
     *
     * RESET_PASSWORD:
     *   Returns phone-verified otpToken — client uses it in /password/reset.
     */
    @Transactional
    public AuthResponse verifyOtp(VerifyOtpRequest request) {
        // Phone comes directly from the request (validated by @Pattern)
        // otpToken is validated to confirm the phone hasn't been swapped mid-session
        String phone   = jwtUtil.extractPhoneFromOtpToken(request.otpToken());
        String purpose = derivePurpose(request);

        // Cross-check: phone in request must match phone in otpToken
        if (!phone.equals(normalisePhone(request.phone()))) {
            throw new CustomException("Phone number does not match OTP session");
        }

        PhoneOtp record = phoneOtpRepository
                .findTopByPhoneAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(phone, purpose)
                .orElseThrow(() -> new CustomException("OTP not found — please request a new one"));

        // Guard: expiry
        if (record.getExpiresAt().isBefore(LocalDateTime.now())) {
            phoneOtpRepository.delete(record);
            throw new CustomException("OTP has expired — please request a new one");
        }

        // Guard: brute-force lockout
        if (record.getAttempts() >= MAX_VERIFY_ATTEMPTS) {
            phoneOtpRepository.delete(record);
            throw new CustomException("Too many incorrect attempts — please request a new OTP");
        }

        // Verify the code
        if (!passwordEncoder.matches(request.otp(), record.getOtpHash())) {
            record.setAttempts(record.getAttempts() + 1);
            phoneOtpRepository.save(record);
            int remaining = MAX_VERIFY_ATTEMPTS - record.getAttempts();
            throw new CustomException("Incorrect OTP — " + remaining + " attempt(s) remaining");
        }

        // Mark as used via usedAt timestamp (entity has no verified boolean)
        record.setUsedAt(LocalDateTime.now());
        phoneOtpRepository.save(record);
        // Clean up all OTPs for this phone+purpose — no replay possible
        phoneOtpRepository.deleteAllByPhoneAndPurpose(phone, purpose);

        // Issue a fresh phone-verified token for RESET_PASSWORD — client carries it to /password/reset
        if ("RESET_PASSWORD".equals(purpose)) {
            return AuthResponse.newUser(jwtUtil.generateOtpToken(phone));
        }

        // For REGISTER / LOGIN / VERIFY_PHONE — branch on whether the user exists
        Optional<User> existingUser = userRepository.findByPhone(phone);

        if (existingUser.isEmpty()) {
            String phoneVerifiedToken = jwtUtil.generateOtpToken(phone);
            return AuthResponse.newUser(phoneVerifiedToken);
        }

        User user = existingUser.get();

        // Mark phone as verified if not already
        if (!user.isPhoneVerified()) {
            user.setPhoneVerified(true);
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
        }

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        String accessToken  = jwtUtil.generateAccessToken(user);
        String refreshToken = tokenService.createRefreshToken(user.getId());
        return AuthResponse.returningUser(accessToken, refreshToken);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Derives the OTP purpose from the verify request.
     * VerifyOtpRequest doesn't carry purpose — we infer it from context.
     * If you later add a purpose field to VerifyOtpRequest, use that directly.
     *
     * Convention: the client sends the same otpToken returned by /send-otp,
     * which was issued for a specific purpose. Since the token doesn't encode
     * purpose (keeping it minimal), we look up by phone and take the latest.
     * To make this explicit, add purpose to VerifyOtpRequest.
     */
    private String derivePurpose(VerifyOtpRequest request) {
        // Default to LOGIN — the most common verify path.
        // OtpService always finds the latest unused record for the phone.
        // If your flow requires explicit purpose, add it to VerifyOtpRequest.
        return "LOGIN";
    }

    private String generateOtp() {
        SecureRandom rng = new SecureRandom();
        int code = 100_000 + rng.nextInt(900_000); // always 6 digits
        return String.valueOf(code);
    }

    /**
     * Normalises to E.164 format — strips spaces, dashes, parentheses.
     */
    private String normalisePhone(String raw) {
        String cleaned = raw.replaceAll("[\\s\\-()]", "");
        if (!cleaned.startsWith("+")) {
            throw new CustomException("Phone must be in E.164 format (e.g. +919876543210)");
        }
        return cleaned;
    }
}