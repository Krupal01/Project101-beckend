package com.crm.authentication.dto;

public record SendOtpResponse(

        // Short-lived, opaque reference token (e.g. signed JWT or UUID stored in cache)
        // Binds the OTP verify call to the send-otp call — prevents cross-session replay
        String otpToken
) {}
