package com.crm.authentication.dto;

public record TokenRefreshResponse(

        String accessToken,

        // seconds until the new access token expires (e.g. 900 for 15 min)
        long expiresIn
) {}
