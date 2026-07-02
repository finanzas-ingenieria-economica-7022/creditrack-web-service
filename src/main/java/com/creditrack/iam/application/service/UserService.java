package com.creditrack.iam.application.service;

import com.creditrack.iam.domain.model.Profile;
import com.creditrack.iam.domain.model.Role;
import com.creditrack.iam.domain.model.User;
import com.creditrack.iam.domain.repositories.PasswordResetTokenRepository;
import com.creditrack.iam.domain.repositories.ProfileRepository;
import com.creditrack.iam.domain.repositories.RefreshTokenRepository;
import com.creditrack.iam.domain.repositories.TokenBlacklistRepository;
import com.creditrack.iam.domain.repositories.UserRepository;
import com.creditrack.iam.interfaces.rest.dto.CreateUserRequest;
import com.creditrack.iam.interfaces.rest.dto.ProfileUpdateRequest;
import com.creditrack.iam.interfaces.rest.dto.UpdateUserRequest;
import com.creditrack.iam.interfaces.rest.dto.UserResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final TokenBlacklistRepository tokenBlacklistRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       ProfileRepository profileRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordResetTokenRepository passwordResetTokenRepository,
                       TokenBlacklistRepository tokenBlacklistRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.tokenBlacklistRepository = tokenBlacklistRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return userRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        return toResponse(findUserByIdOrThrow(id));
    }

    @Transactional(readOnly = true)
    public UserResponse findByUsername(String username) {
        return toResponse(findUserByUsernameOrThrow(username));
    }

    @Transactional(readOnly = true)
    public User findUserByUsernameOrThrow(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    @Transactional(readOnly = true)
    public User findUserByEmailOrThrow(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    @Transactional(readOnly = true)
    public User findUserByIdOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    public User createUserEntity(CreateUserRequest request, String roleValue) {
        String username = requireText(request.getUsername(), "username");
        String email = requireText(request.getEmail(), "email");
        String password = requireText(request.getPassword(), "password");
        String role = resolveRole(roleValue);
        validateUniqueConstraints(username, email);

        User user = new User(
                null,
                username,
                passwordEncoder.encode(password),
                email,
                role,
                request.getEnabled() == null ? Boolean.TRUE : request.getEnabled(),
                0
        );

        User savedUser = userRepository.save(user);
        saveProfile(savedUser.getId(),
                request.getFirstName(),
                request.getLastName(),
                request.getDocumentType(),
                request.getDocumentNumber(),
                request.getPhoneNumber());

        return savedUser;
    }

    public UserResponse createUser(CreateUserRequest request) {
        return toResponse(createUserEntity(request, request.getRole()));
    }

    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        User user = findUserByIdOrThrow(id);

        if (StringUtils.hasText(request.getUsername())
                && !user.getUsername().equalsIgnoreCase(request.getUsername())
                && userRepository.existsByUsername(request.getUsername())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username is already taken");
        }

        if (StringUtils.hasText(request.getEmail())
                && !user.getEmail().equalsIgnoreCase(request.getEmail())
                && userRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already taken");
        }

        if (StringUtils.hasText(request.getUsername())) {
            user.setUsername(request.getUsername().trim());
        }
        if (StringUtils.hasText(request.getEmail())) {
            user.setEmail(request.getEmail().trim());
        }
        if (StringUtils.hasText(request.getPassword())) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        if (StringUtils.hasText(request.getRole())) {
            user.setRole(resolveRole(request.getRole()));
        }
        if (request.getEnabled() != null) {
            user.setEnabled(request.getEnabled());
        }

        userRepository.save(user);
        upsertProfile(user.getId(),
                request.getFirstName(),
                request.getLastName(),
                request.getDocumentType(),
                request.getDocumentNumber(),
                request.getPhoneNumber());

        return toResponse(user);
    }

    public UserResponse updateMyProfile(String username, ProfileUpdateRequest request) {
        User user = findUserByUsernameOrThrow(username);
        upsertProfile(user.getId(),
                request.getFirstName(),
                request.getLastName(),
                request.getDocumentType(),
                request.getDocumentNumber(),
                request.getPhoneNumber());

        return toResponse(user);
    }

    public void changePassword(User user, String rawPassword) {
        user.setPassword(passwordEncoder.encode(requireText(rawPassword, "newPassword")));
        bumpTokenVersion(user);
        refreshTokenRepository.deleteAllByUserId(user.getId());
        passwordResetTokenRepository.deleteAllByUserId(user.getId());
    }

    public void deleteUser(Long id) {
        User user = findUserByIdOrThrow(id);
        refreshTokenRepository.deleteAllByUserId(user.getId());
        passwordResetTokenRepository.deleteAllByUserId(user.getId());
        tokenBlacklistRepository.deleteAllByUserId(user.getId());
        profileRepository.deleteByUserId(user.getId());
        userRepository.delete(user);
    }

    public void bumpTokenVersion(User user) {
        int tokenVersion = user.getTokenVersion() == null ? 0 : user.getTokenVersion();
        user.setTokenVersion(tokenVersion + 1);
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public Profile findProfileByUserId(Long userId) {
        return profileRepository.findByUserId(userId)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public UserResponse toResponse(User user) {
        Profile profile = findProfileByUserId(user.getId());
        return UserResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .enabled(Boolean.TRUE.equals(user.getEnabled()))
                .tokenVersion(user.getTokenVersion() == null ? 0 : user.getTokenVersion())
                .profileId(profile == null ? null : profile.getId())
                .firstName(profile == null ? null : profile.getFirstName())
                .lastName(profile == null ? null : profile.getLastName())
                .documentType(profile == null ? null : profile.getDocumentType())
                .documentNumber(profile == null ? null : profile.getDocumentNumber())
                .phoneNumber(profile == null ? null : profile.getPhoneNumber())
                .build();
    }

    private void validateUniqueConstraints(String username, String email) {
        if (StringUtils.hasText(username) && userRepository.existsByUsername(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username is already taken");
        }
        if (StringUtils.hasText(email) && userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already taken");
        }
    }

    private String resolveRole(String roleValue) {
        if (!StringUtils.hasText(roleValue)) {
            return Role.ROLE_USER.name();
        }

        for (Role role : Role.values()) {
            if (role.name().equalsIgnoreCase(roleValue)) {
                return role.name();
            }
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role");
    }

    private void saveProfile(Long userId,
                             String firstName,
                             String lastName,
                             String documentType,
                             String documentNumber,
                             String phoneNumber) {
        if (!StringUtils.hasText(firstName)
                || !StringUtils.hasText(lastName)
                || !StringUtils.hasText(documentType)
                || !StringUtils.hasText(documentNumber)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Profile information is required");
        }

        Profile profile = new Profile(
                null,
                firstName.trim(),
                lastName.trim(),
                documentType.trim(),
                documentNumber.trim(),
                StringUtils.hasText(phoneNumber) ? phoneNumber.trim() : null,
                userId
        );
        profileRepository.save(profile);
    }

    private void upsertProfile(Long userId,
                               String firstName,
                               String lastName,
                               String documentType,
                               String documentNumber,
                               String phoneNumber) {
        Profile profile = profileRepository.findByUserId(userId).orElse(null);
        if (profile == null) {
            if (!StringUtils.hasText(firstName)
                    || !StringUtils.hasText(lastName)
                    || !StringUtils.hasText(documentType)
                    || !StringUtils.hasText(documentNumber)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Profile information is required");
            }
            profile = new Profile();
            profile.setUserId(userId);
        }

        if (StringUtils.hasText(firstName)) {
            profile.setFirstName(firstName.trim());
        }
        if (StringUtils.hasText(lastName)) {
            profile.setLastName(lastName.trim());
        }
        if (StringUtils.hasText(documentType)) {
            profile.setDocumentType(documentType.trim());
        }
        if (StringUtils.hasText(documentNumber)) {
            profile.setDocumentNumber(documentNumber.trim());
        }
        if (phoneNumber != null) {
            profile.setPhoneNumber(StringUtils.hasText(phoneNumber) ? phoneNumber.trim() : null);
        }

        profileRepository.save(profile);
    }

    private String requireText(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " is required");
        }
        return value.trim();
    }
}
