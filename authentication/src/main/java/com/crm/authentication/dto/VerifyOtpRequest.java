package com.crm.authentication.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record VerifyOtpRequest(

        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
        @Size(max = 15, message = "Phone number must not exceed 15 characters")
        String phone,

        @NotBlank(message = "OTP is required")
        @Pattern(regexp = "^\\d{4,8}$", message = "OTP must be 4–8 digits")
        String otp,

        @NotBlank(message = "OTP token is required")
        String otpToken
) {}
