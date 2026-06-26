package com.creditrack.iam.interfaces.rest;

import com.creditrack.iam.domain.model.Profile;
import com.creditrack.iam.domain.model.User;
import com.creditrack.iam.domain.repositories.ProfileRepository;
import com.creditrack.iam.domain.repositories.UserRepository;
import com.creditrack.iam.infrastructure.security.JwtTokenProvider;
import com.creditrack.iam.interfaces.rest.dto.LoginRequest;
import com.creditrack.iam.interfaces.rest.dto.LoginResponse;
import com.creditrack.iam.interfaces.rest.dto.RegisterRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthController(AuthenticationManager authenticationManager,
                          UserRepository userRepository,
                          ProfileRepository profileRepository,
                          PasswordEncoder passwordEncoder,
                          JwtTokenProvider tokenProvider) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody RegisterRequest registerRequest) {
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            return ResponseEntity.badRequest().body("Error: Username is already taken!");
        }

        // Create user
        User user = new User(
                null,
                registerRequest.getUsername(),
                passwordEncoder.encode(registerRequest.getPassword()),
                registerRequest.getEmail(),
                "ROLE_USER"
        );
        User savedUser = userRepository.save(user);

        // Create profile
        Profile profile = new Profile(
                null,
                registerRequest.getFirstName(),
                registerRequest.getLastName(),
                registerRequest.getDocumentType(),
                registerRequest.getDocumentNumber(),
                registerRequest.getPhoneNumber(),
                savedUser.getId()
        );
        profileRepository.save(profile);

        return ResponseEntity.ok("User registered successfully!");
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken(authentication);

        return ResponseEntity.ok(new LoginResponse(jwt, "Bearer", loginRequest.getUsername()));
    }
}
