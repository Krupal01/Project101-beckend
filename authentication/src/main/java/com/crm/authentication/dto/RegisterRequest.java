package com.crm.authentication.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        // Phone is re-sent so the server can match it against the verified OTP session
        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
        @Size(max = 15, message = "Phone number must not exceed 15 characters")
        String phone,

        // otpToken proves the phone was verified in the send-otp → verify-otp step
        // Without this, anyone could register with an arbitrary phone number
        @NotBlank(message = "OTP token is required")
        String otpToken,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        @Size(max = 150, message = "Email must not exceed 150 characters")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#^()_+=\\-]).+$",
                message = "Password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character"
        )
        String password
) {}
