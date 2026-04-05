package com.crm.authentication.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ChangePasswordRequest(

        @NotBlank(message = "Current password is required")
        String currentPassword,

        @NotBlank(message = "New password is required")
        @Size(min = 8, max = 128, message = "New password must be between 8 and 128 characters")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#^()_+=\\-]).+$",
                message = "New password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character"
        )
        String newPassword,

        UUID userId
) {}
