package com.crm.authentication.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AppleAuthRequest(

        @NotBlank(message = "Apple ID token is required")
        String idToken,

        // Optional — only sent on first sign-in; Apple doesn't re-send it
        @Size(max = 150, message = "Full name must not exceed 150 characters")
        String fullName
) {}
