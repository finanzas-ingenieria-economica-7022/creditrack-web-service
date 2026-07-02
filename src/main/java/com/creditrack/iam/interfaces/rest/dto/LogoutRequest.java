package com.creditrack.iam.interfaces.rest.dto;

import lombok.Data;

@Data
public class LogoutRequest {
    private String refreshToken;
}
