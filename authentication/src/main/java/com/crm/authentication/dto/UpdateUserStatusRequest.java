package com.crm.authentication.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateUserStatusRequest(

        @NotBlank(message = "Status is required")
        @Pattern(
                regexp = "^(ACTIVE|SUSPENDED|BANNED|DEACTIVATED)$",
                message = "Status must be one of: ACTIVE, SUSPENDED, BANNED, DEACTIVATED"
        )
        String status
) {}
