package com.crm.authentication.dto;

/**
 * Verified payload extracted from an Apple ID token.
 * Populate this inside JwtUtil.verifyAppleToken() by verifying
 * the JWT against Apple's JWKS endpoint (https://appleid.apple.com/auth/keys).
 *
 * Note: email is only present on the first sign-in. On subsequent sign-ins
 * Apple omits it — appleUid (the "sub" claim) is the stable identifier.
 */
public record AppleTokenPayload(

        // Maps to User.appleUid — the "sub" claim in Apple's JWT, stable across sign-ins
        String appleUid,

        // Null on repeat sign-ins — always check before using
        String email
) {}