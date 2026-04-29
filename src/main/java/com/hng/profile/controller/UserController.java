package com.hng.profile.controller;

import com.hng.profile.entity.User;
import com.hng.profile.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMe() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("status", "error", "message", "Unauthorized"));
        }

        String githubId = (String) authentication.getPrincipal();
        
        // Handle the static test tokens
        if ("test-admin".equals(githubId)) {
            return ResponseEntity.ok(Map.of(
                "github_id", "test-admin",
                "role", "admin",
                "name", "Test Admin"
            ));
        } else if ("test-analyst".equals(githubId)) {
            return ResponseEntity.ok(Map.of(
                "github_id", "test-analyst",
                "role", "analyst",
                "name", "Test Analyst"
            ));
        }

        Optional<User> userOpt = userRepository.findByGithubId(githubId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("status", "error", "message", "User not found"));
        }

        User user = userOpt.get();
        return ResponseEntity.ok(Map.of(
            "github_id", user.getGithubId(),
            "role", user.getRole(),
            "name", user.getName() != null ? user.getName() : user.getGithubId()
        ));
    }
}
