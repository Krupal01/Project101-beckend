package com.crm.authentication.dto;


import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class RefreshTokenRequest {
    private String refreshToken;
}
