package com.crm.authentication.dto;

public record AuthResponse(

        String accessToken,

        String refreshToken,

        // true  → client should redirect to onboarding / profile setup
        // false → returning user, go to home
        boolean isNewUser
) {
    public static AuthResponse newUser(String otpToken) {
        return new AuthResponse(otpToken, null, true);
    }

    /**
     * Returned by /verify-otp (or any login endpoint) for existing users.
     */
    public static AuthResponse returningUser(String accessToken, String refreshToken) {
        return new AuthResponse(accessToken, refreshToken, false);
    }
}
