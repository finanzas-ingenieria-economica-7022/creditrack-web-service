package com.creditrack.iam.interfaces.rest.dto;

import lombok.Data;

@Data
public class RefreshTokenRequest {
    private String refreshToken;
}
