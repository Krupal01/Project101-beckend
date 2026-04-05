package com.crm.authentication.dto;

/**
 * Verified payload extracted from a Google ID token.
 * Populate this inside JwtUtil.verifyGoogleToken() using the Google API client
 * or by verifying the JWT against Google's public JWKS endpoint.
 */
public record GoogleTokenPayload(

        // Maps to User.googleUid
        String googleUid,

        String email,

        // Google guarantees email addresses are pre-verified
        boolean emailVerified
) {}