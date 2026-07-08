package com.creditrack.iam.interfaces.rest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long userId;
    private String username;
    private String email;
    private String role;
    private Boolean enabled;
    private Integer tokenVersion;
    private Long profileId;
    private String firstName;
    private String lastName;
    private String documentType;
    private String documentNumber;
    private String phoneNumber;
}
