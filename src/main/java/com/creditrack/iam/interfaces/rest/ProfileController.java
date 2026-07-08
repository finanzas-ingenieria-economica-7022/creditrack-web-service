package com.creditrack.iam.interfaces.rest;

import com.creditrack.iam.application.service.UserService;
import com.creditrack.iam.interfaces.rest.dto.ProfileResponse;
import com.creditrack.iam.interfaces.rest.dto.ProfileUpdateRequest;
import com.creditrack.iam.interfaces.rest.dto.UserResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profiles")
@Tag(name = "2. User Profiles", description = "Authenticate and retrieve user profile details")
public class ProfileController {

    private final UserService userService;

    public ProfileController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    @PreAuthorize("hasAuthority('profile:read')")
    public ResponseEntity<ProfileResponse> getMyProfile(Authentication authentication) {
        UserResponse user = userService.findByUsername(authentication.getName());
        return ResponseEntity.ok(toProfileResponse(user));
    }

    @PutMapping("/me")
    @PreAuthorize("hasAuthority('profile:update')")
    public ResponseEntity<ProfileResponse> updateMyProfile(Authentication authentication,
                                                           @RequestBody ProfileUpdateRequest request) {
        UserResponse user = userService.updateMyProfile(authentication.getName(), request);
        return ResponseEntity.ok(toProfileResponse(user));
    }

    private ProfileResponse toProfileResponse(UserResponse user) {
        return ProfileResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .enabled(user.getEnabled())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .documentType(user.getDocumentType())
                .documentNumber(user.getDocumentNumber())
                .phoneNumber(user.getPhoneNumber())
                .build();
    }
}
