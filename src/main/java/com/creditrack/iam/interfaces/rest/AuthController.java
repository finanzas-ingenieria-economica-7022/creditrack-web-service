package com.creditrack.iam.interfaces.rest;

import com.creditrack.iam.application.service.AuthService;
import com.creditrack.iam.interfaces.rest.dto.ChangePasswordRequest;
import com.creditrack.iam.interfaces.rest.dto.ForgotPasswordRequest;
import com.creditrack.iam.interfaces.rest.dto.ForgotPasswordResponse;
import com.creditrack.iam.interfaces.rest.dto.LoginRequest;
import com.creditrack.iam.interfaces.rest.dto.LoginResponse;
import com.creditrack.iam.interfaces.rest.dto.LogoutRequest;
import com.creditrack.iam.interfaces.rest.dto.RefreshTokenRequest;
import com.creditrack.iam.interfaces.rest.dto.RegisterRequest;
import com.creditrack.iam.interfaces.rest.dto.ResetPasswordRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "1. Authentication", description = "User registration and authentication endpoints")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<LoginResponse> registerUser(@RequestBody RegisterRequest registerRequest) {
        return ResponseEntity.ok(authService.register(registerRequest));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> authenticateUser(@RequestBody LoginRequest loginRequest) {
        return ResponseEntity.ok(authService.login(loginRequest));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<LoginResponse> refreshToken(@RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorizationHeader,
                                       @RequestBody(required = false) LogoutRequest request) {
        authService.logout(authorizationHeader, request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout-all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> logoutAll(Authentication authentication,
                                          @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        authService.logoutAll(authentication.getName(), authorizationHeader);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ForgotPasswordResponse> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(authService.forgotPassword(request));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/change-password")
    @PreAuthorize("hasAuthority('password:change')")
    public ResponseEntity<Void> changePassword(Authentication authentication, @RequestBody ChangePasswordRequest request) {
        authService.changePassword(authentication.getName(), request);
        return ResponseEntity.noContent().build();
    }
}
