package com.creditrack.iam.interfaces.rest;

import com.creditrack.iam.domain.model.Profile;
import com.creditrack.iam.domain.model.User;
import com.creditrack.iam.domain.repositories.ProfileRepository;
import com.creditrack.iam.domain.repositories.UserRepository;
import com.creditrack.iam.interfaces.rest.dto.ProfileResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profiles")
@Tag(name = "2. User Profiles", description = "Authenticate and retrieve user profile details")
public class ProfileController {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;

    public ProfileController(UserRepository userRepository, ProfileRepository profileRepository) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMyProfile() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String username;
        if (principal instanceof UserDetails) {
            username = ((UserDetails) principal).getUsername();
        } else {
            username = principal.toString();
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Logged in user not found in DB"));

        Profile profile = profileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Profile not found for user"));

        ProfileResponse response = new ProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                profile.getFirstName(),
                profile.getLastName(),
                profile.getDocumentType(),
                profile.getDocumentNumber(),
                profile.getPhoneNumber()
        );

        return ResponseEntity.ok(response);
    }
}
