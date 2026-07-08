package com.creditrack.iam.application.service;

import com.creditrack.iam.domain.model.PasswordResetToken;
import com.creditrack.iam.domain.model.Permission;
import com.creditrack.iam.domain.model.RefreshToken;
import com.creditrack.iam.domain.model.Role;
import com.creditrack.iam.domain.model.TokenBlacklist;
import com.creditrack.iam.domain.model.User;
import com.creditrack.iam.domain.repositories.PasswordResetTokenRepository;
import com.creditrack.iam.domain.repositories.RefreshTokenRepository;
import com.creditrack.iam.domain.repositories.TokenBlacklistRepository;
import com.creditrack.iam.domain.repositories.UserRepository;
import com.creditrack.iam.infrastructure.security.JwtTokenProvider;
import com.creditrack.iam.interfaces.rest.dto.ChangePasswordRequest;
import com.creditrack.iam.interfaces.rest.dto.ForgotPasswordRequest;
import com.creditrack.iam.interfaces.rest.dto.ForgotPasswordResponse;
import com.creditrack.iam.interfaces.rest.dto.CreateUserRequest;
import com.creditrack.iam.interfaces.rest.dto.LoginRequest;
import com.creditrack.iam.interfaces.rest.dto.LoginResponse;
import com.creditrack.iam.interfaces.rest.dto.LogoutRequest;
import com.creditrack.iam.interfaces.rest.dto.RefreshTokenRequest;
import com.creditrack.iam.interfaces.rest.dto.RegisterRequest;
import com.creditrack.iam.interfaces.rest.dto.ResetPasswordRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

@Service
@Transactional
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final TokenBlacklistRepository tokenBlacklistRepository;
    private final JwtTokenProvider tokenProvider;
    private final long refreshTokenExpirationInMs;
    private final long accessTokenExpirationInMs;
    private final long resetTokenExpirationInMs;

    public AuthService(AuthenticationManager authenticationManager,
                       UserService userService,
                       UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordResetTokenRepository passwordResetTokenRepository,
                       TokenBlacklistRepository tokenBlacklistRepository,
                       JwtTokenProvider tokenProvider,
                       @Value("${jwt.refresh-expiration}") long refreshTokenExpirationInMs,
                       @Value("${jwt.expiration}") long accessTokenExpirationInMs,
                       @Value("${jwt.reset-expiration}") long resetTokenExpirationInMs) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.tokenBlacklistRepository = tokenBlacklistRepository;
        this.tokenProvider = tokenProvider;
        this.refreshTokenExpirationInMs = refreshTokenExpirationInMs;
        this.accessTokenExpirationInMs = accessTokenExpirationInMs;
        this.resetTokenExpirationInMs = resetTokenExpirationInMs;
    }

    public LoginResponse register(RegisterRequest request) {
        User user = userService.createUserEntity(toCreateUserRequest(request), Role.ROLE_USER.name());
        return buildAuthResponse(user);
    }

    public LoginResponse login(LoginRequest request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        } catch (AuthenticationException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }
        UserDetails principal = (UserDetails) authentication.getPrincipal();
        User user = userService.findUserByUsernameOrThrow(principal.getUsername());
        return buildAuthResponse(user);
    }

    public LoginResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken storedRefreshToken = findActiveRefreshToken(request.getRefreshToken());
        User user = userService.findUserByIdOrThrow(storedRefreshToken.getUserId());

        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User account is disabled");
        }

        if (!refreshTokenVersionMatches(storedRefreshToken, user)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token is no longer valid");
        }

        storedRefreshToken.setRevoked(true);
        storedRefreshToken.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(storedRefreshToken);

        return buildAuthResponse(user);
    }

    public void logout(String authorizationHeader, LogoutRequest request) {
        if (request != null && StringUtils.hasText(request.getRefreshToken())) {
            revokeRefreshToken(request.getRefreshToken());
        }

        if (StringUtils.hasText(authorizationHeader) && authorizationHeader.startsWith("Bearer ")) {
            String accessToken = authorizationHeader.substring(7);
            if (tokenProvider.validateToken(accessToken)) {
                blacklistAccessToken(accessToken);
            }
        }
    }

    public void logoutAll(String username) {
        logoutAll(username, null);
    }

    public void logoutAll(String username, String authorizationHeader) {
        User user = userService.findUserByUsernameOrThrow(username);
        userService.bumpTokenVersion(user);
        refreshTokenRepository.deleteAllByUserId(user.getId());
        tokenBlacklistRepository.deleteAllByUserId(user.getId());
        if (StringUtils.hasText(authorizationHeader) && authorizationHeader.startsWith("Bearer ")) {
            blacklistAccessToken(authorizationHeader.substring(7));
        }
    }

    public ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request) {
        if (!StringUtils.hasText(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "email is required");
        }

        User user = userRepository.findByEmail(request.getEmail().trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        PasswordResetToken resetToken = new PasswordResetToken(
                null,
                UUID.randomUUID().toString(),
                user.getId(),
                LocalDateTime.now().plus(Duration.ofMillis(resetTokenExpirationInMs)),
                false,
                null,
                null
        );
        passwordResetTokenRepository.save(resetToken);

        return ForgotPasswordResponse.builder()
                .message("Password reset token generated successfully")
                .resetToken(resetToken.getToken())
                .expiresAt(resetToken.getExpiresAt())
                .build();
    }

    public void resetPassword(ResetPasswordRequest request) {
        if (!StringUtils.hasText(request.getToken()) || !StringUtils.hasText(request.getNewPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "token and newPassword are required");
        }

        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid reset token"));

        if (Boolean.TRUE.equals(resetToken.getUsed()) || resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reset token is expired or already used");
        }

        User user = userService.findUserByIdOrThrow(resetToken.getUserId());
        // Mark the token as used before changePassword, which deletes every reset
        // token of the user; saving afterwards merges a deleted entity and fails
        resetToken.setUsed(true);
        resetToken.setUsedAt(LocalDateTime.now());
        passwordResetTokenRepository.save(resetToken);
        userService.changePassword(user, request.getNewPassword());
    }

    public void changePassword(String username, ChangePasswordRequest request) {
        if (!StringUtils.hasText(request.getCurrentPassword()) || !StringUtils.hasText(request.getNewPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "currentPassword and newPassword are required");
        }

        User user = userService.findUserByUsernameOrThrow(username);
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, request.getCurrentPassword()));
            if (authentication == null || !authentication.isAuthenticated()) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid current password");
            }
        } catch (AuthenticationException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid current password");
        }

        userService.changePassword(user, request.getNewPassword());
    }

    private LoginResponse buildAuthResponse(User user) {
        String accessToken = tokenProvider.generateToken(user.getUsername(), user.getTokenVersion() == null ? 0 : user.getTokenVersion());
        RefreshToken refreshToken = createRefreshToken(user);
        LocalDateTime accessTokenExpiresAt = LocalDateTime.now().plus(Duration.ofMillis(accessTokenExpirationInMs));
        Role role = Role.fromValue(user.getRole());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .expiresAt(accessTokenExpiresAt)
                .permissions(role.getPermissions().stream().map(Permission::getAuthority).toList())
                .build();
    }

    private RefreshToken createRefreshToken(User user) {
        RefreshToken refreshToken = new RefreshToken(
                null,
                UUID.randomUUID().toString(),
                user.getId(),
                user.getTokenVersion() == null ? 0 : user.getTokenVersion(),
                LocalDateTime.now().plus(Duration.ofMillis(refreshTokenExpirationInMs)),
                false,
                null,
                null
        );
        return refreshTokenRepository.save(refreshToken);
    }

    private RefreshToken findActiveRefreshToken(String token) {
        if (!StringUtils.hasText(token)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "refreshToken is required");
        }

        RefreshToken refreshToken = refreshTokenRepository.findByToken(token.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token is invalid"));

        if (Boolean.TRUE.equals(refreshToken.getRevoked()) || refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token is expired or revoked");
        }

        return refreshToken;
    }

    private boolean refreshTokenVersionMatches(RefreshToken refreshToken, User user) {
        int currentVersion = user.getTokenVersion() == null ? 0 : user.getTokenVersion();
        return refreshToken.getTokenVersion() != null && refreshToken.getTokenVersion() == currentVersion;
    }

    private void revokeRefreshToken(String token) {
        refreshTokenRepository.findByToken(token.trim()).ifPresent(stored -> {
            stored.setRevoked(true);
            stored.setRevokedAt(LocalDateTime.now());
            refreshTokenRepository.save(stored);
        });
    }

    private void blacklistAccessToken(String accessToken) {
        try {
            String jti = tokenProvider.getJtiFromJWT(accessToken);
            if (!StringUtils.hasText(jti)) {
                return;
            }

            String username = tokenProvider.getUsernameFromJWT(accessToken);
            User user = userRepository.findByUsername(username).orElse(null);
            Long userId = user == null ? null : user.getId();

            tokenBlacklistRepository.findByJti(jti).ifPresentOrElse(existing -> {
                existing.setExpiresAt(getAccessTokenExpiration(accessToken));
                existing.setUserId(userId);
                tokenBlacklistRepository.save(existing);
            }, () -> tokenBlacklistRepository.save(new TokenBlacklist(
                    null,
                    jti,
                    userId,
                    getAccessTokenExpiration(accessToken),
                    null
            )));
        } catch (Exception ignored) {
            // Ignore malformed or already expired access tokens during logout flows.
        }
    }

    private LocalDateTime getAccessTokenExpiration(String accessToken) {
        try {
            Date expiration = tokenProvider.getExpirationDateFromJWT(accessToken);
            return LocalDateTime.ofInstant(expiration.toInstant(), java.time.ZoneId.systemDefault());
        } catch (Exception ex) {
            return LocalDateTime.now().plus(Duration.ofMillis(accessTokenExpirationInMs));
        }
    }

    private CreateUserRequest toCreateUserRequest(RegisterRequest request) {
        CreateUserRequest createUserRequest = new CreateUserRequest();
        createUserRequest.setUsername(request.getUsername());
        createUserRequest.setPassword(request.getPassword());
        createUserRequest.setEmail(request.getEmail());
        createUserRequest.setFirstName(request.getFirstName());
        createUserRequest.setLastName(request.getLastName());
        createUserRequest.setDocumentType(request.getDocumentType());
        createUserRequest.setDocumentNumber(request.getDocumentNumber());
        createUserRequest.setPhoneNumber(request.getPhoneNumber());
        return createUserRequest;
    }
}
