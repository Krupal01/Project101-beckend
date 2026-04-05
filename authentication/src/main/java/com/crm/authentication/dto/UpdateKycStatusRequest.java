package com.crm.authentication.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateKycStatusRequest(

        @NotBlank(message = "KYC status is required")
        @Pattern(
                regexp = "^(NONE|PENDING|APPROVED|REJECTED)$",
                message = "KYC status must be one of: NONE, PENDING, APPROVED, REJECTED"
        )
        String kycStatus
) {}
