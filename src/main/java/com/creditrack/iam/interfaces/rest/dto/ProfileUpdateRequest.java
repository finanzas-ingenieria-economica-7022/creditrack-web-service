package com.creditrack.iam.interfaces.rest.dto;

import lombok.Data;

@Data
public class ProfileUpdateRequest {
    private String firstName;
    private String lastName;
    private String documentType;
    private String documentNumber;
    private String phoneNumber;
}
