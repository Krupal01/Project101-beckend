package com.crm.authentication.utils;

import com.crm.authentication.configure.JwtConfig;
import com.crm.authentication.dto.AppleTokenPayload;
import com.crm.authentication.dto.GoogleTokenPayload;
import com.crm.authentication.entity.User;
import com.crm.authentication.exception.CustomException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.crypto.SecretKey;
import java.math.BigInteger;
import java.net.URI;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtUtil {

    private static final long   OTP_TOKEN_EXPIRY_MS = 10 * 60 * 1000L; // 10 minutes
    private static final String OTP_TOKEN_TYPE      = "otp";
    private static final String CLAIM_TOKEN_TYPE    = "type";

    private final JwtConfig    jwtConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // ─────────────────────────────────────────────────────────────────────────
    // Access token
    // ─────────────────────────────────────────────────────────────────────────

    public String generateAccessToken(User user) {
        Date now    = new Date();
        Date expiry = new Date(now.getTime() + jwtConfig.getAccessTokenExpiration());

        return Jwts.builder()
                .subject(user.getId().toString())        // UUID as string subject
                .claim("phone", user.getPhone())
                .claim("email", user.getEmail())
                .claim("role", user.getRole())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(jwtConfig.getSigningKey())
                .compact();
    }

    /**
     * Extracts the user UUID from an access token subject.
     * Subject is stored as a UUID string — never a Long.
     */
    public UUID extractUserId(String token) {
        Claims claims = parseClaims(token);
        return UUID.fromString(claims.getSubject());
    }

    public boolean validateToken(String token) {
        try {
            Claims claims = parseClaims(token);
            // Reject OTP tokens passed as access tokens
            String type = claims.get(CLAIM_TOKEN_TYPE, String.class);
            return !OTP_TOKEN_TYPE.equals(type);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // OTP token — short-lived, phone-scoped, type-discriminated
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Issues a short-lived JWT binding a phone number to an OTP session.
     * Returned by /send-otp and re-issued as phone-verified proof after /verify-otp.
     */
    public String generateOtpToken(String phone) {
        Date now    = new Date();
        Date expiry = new Date(now.getTime() + OTP_TOKEN_EXPIRY_MS);

        return Jwts.builder()
                .subject(phone)
                .claim(CLAIM_TOKEN_TYPE, OTP_TOKEN_TYPE)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(jwtConfig.getSigningKey())
                .compact();
    }

    /**
     * Extracts and validates the phone number from an OTP token.
     * Rejects expired, tampered, or wrong token-type tokens.
     */
    public String extractPhoneFromOtpToken(String otpToken) {
        Claims claims;
        try {
            claims = parseClaims(otpToken);
        } catch (JwtException | IllegalArgumentException e) {
            throw new CustomException("Invalid or expired OTP session — please request a new OTP");
        }

        String type = claims.get(CLAIM_TOKEN_TYPE, String.class);
        if (!OTP_TOKEN_TYPE.equals(type)) {
            throw new CustomException("Invalid token type for OTP verification");
        }

        return claims.getSubject(); // phone in E.164 format
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Social — Google
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Verifies a Google ID token by calling Google's tokeninfo endpoint.
     * Returns a {@link GoogleTokenPayload} with the verified claims.
     *
     * Google tokeninfo endpoint validates the signature, expiry, and audience
     * server-side — no local JWKS fetch needed.
     *
     * Alternative: use the google-api-client library
     *   (com.google.api-client:google-api-client) for offline verification.
     */
    public GoogleTokenPayload verifyGoogleToken(String idToken) {
        String url = "https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken;

        JsonNode response;
        try {
            String raw = restTemplate.getForObject(URI.create(url), String.class);
            response   = objectMapper.readTree(raw);
        } catch (Exception e) {
            throw new CustomException("Failed to verify Google token — please try again");
        }

        // Google returns an "error_description" field on failure
        if (response.has("error_description")) {
            throw new CustomException("Invalid Google token: " + response.get("error_description").asText());
        }

        String googleUid = response.path("sub").asText(null);
        String email     = response.path("email").asText(null);
        boolean verified = "true".equalsIgnoreCase(response.path("email_verified").asText("false"));

        if (googleUid == null || googleUid.isBlank()) {
            throw new CustomException("Google token is missing required claims");
        }

        return new GoogleTokenPayload(googleUid, email, verified);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Social — Apple
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Verifies an Apple ID token by:
     *   1. Fetching Apple's public JWKS from https://appleid.apple.com/auth/keys
     *   2. Matching the token's "kid" header to the correct key
     *   3. Building an RSA public key and verifying the JWT signature + claims locally
     *
     * Apple does NOT offer a server-side tokeninfo endpoint like Google,
     * so local verification against the JWKS is the correct approach.
     *
     * NOTE: In production, cache the JWKS (it changes infrequently) to avoid
     * a network call on every login. A simple @Cacheable on this method works well.
     */
    public AppleTokenPayload verifyAppleToken(String idToken) {
        // ── Step 1: extract the key ID (kid) from the token header ──────────
        String kid = extractKidFromHeader(idToken);

        // ── Step 2: fetch Apple's public JWKS ───────────────────────────────
        JsonNode jwks;
        try {
            String raw = restTemplate.getForObject(
                    URI.create("https://appleid.apple.com/auth/keys"), String.class);
            jwks = objectMapper.readTree(raw).path("keys");
        } catch (Exception e) {
            throw new CustomException("Failed to fetch Apple public keys — please try again");
        }

        // ── Step 3: find the matching key by kid ────────────────────────────
        JsonNode matchedKey = null;
        for (JsonNode key : jwks) {
            if (kid.equals(key.path("kid").asText())) {
                matchedKey = key;
                break;
            }
        }
        if (matchedKey == null) {
            throw new CustomException("Apple token signing key not found");
        }

        // ── Step 4: build the RSA public key ────────────────────────────────
        PublicKey publicKey;
        try {
            BigInteger modulus  = new BigInteger(1,
                    Base64.getUrlDecoder().decode(matchedKey.path("n").asText()));
            BigInteger exponent = new BigInteger(1,
                    Base64.getUrlDecoder().decode(matchedKey.path("e").asText()));

            RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, exponent);
            publicKey = KeyFactory.getInstance("RSA").generatePublic(spec);
        } catch (Exception e) {
            throw new CustomException("Failed to build Apple public key");
        }

        // ── Step 5: verify the JWT signature and parse claims ────────────────
        Claims claims;
        try {
            claims = Jwts.parser()
                    .verifyWith((java.security.interfaces.RSAPublicKey) publicKey)
                    .build()
                    .parseSignedClaims(idToken)
                    .getPayload();
        } catch (JwtException e) {
            throw new CustomException("Apple token verification failed: " + e.getMessage());
        }

        // ── Step 6: validate issuer and extract claims ───────────────────────
        String issuer = claims.getIssuer();
        if (!"https://appleid.apple.com".equals(issuer)) {
            throw new CustomException("Invalid Apple token issuer");
        }

        String appleUid = claims.getSubject();
        String email    = claims.get("email", String.class); // null on repeat sign-ins

        if (appleUid == null || appleUid.isBlank()) {
            throw new CustomException("Apple token is missing required claims");
        }

        return new AppleTokenPayload(appleUid, email);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internal
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Decodes the JWT header (without verifying the signature) to extract the kid.
     * This is safe — we only use the kid to look up the correct public key;
     * the signature is verified in the next step.
     */
    private String extractKidFromHeader(String token) {
        try {
            String headerJson = new String(
                    Base64.getUrlDecoder().decode(token.split("\\.")[0]));
            JsonNode header = objectMapper.readTree(headerJson);
            String kid = header.path("kid").asText(null);
            if (kid == null || kid.isBlank()) {
                throw new CustomException("Apple token missing kid header");
            }
            return kid;
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException("Failed to parse Apple token header");
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith((SecretKey) jwtConfig.getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}