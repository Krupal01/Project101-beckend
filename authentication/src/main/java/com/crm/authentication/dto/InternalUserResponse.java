package com.crm.authentication.dto;

import java.util.UUID;

public record InternalUserResponse(

        UUID id,

        String phone,

        String email,

        String role,

        String globalStatus,

        String kycStatus
) {}
