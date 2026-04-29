package com.hng.profile.controller;

import com.hng.profile.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/github/callback")
  public ResponseEntity<?> githubCallback(@RequestBody Map<String, String> request) {
    String code = request.get("code");
    String codeVerifier = request.get("code_verifier");

    if (code == null || code.isEmpty()) {
      return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Authorization code is required"));
    }

    try {
      Map<String, String> tokens = authService.processGithubCallback(code, codeVerifier);
      return ResponseEntity.ok(Map.of(
          "status", "success",
          "access_token", tokens.get("access_token"),
          "refresh_token", tokens.get("refresh_token")));
    } catch (Exception e) {
      return ResponseEntity.badRequest().body(Map.of("status", "error", "message", e.getMessage()));
    }
  }

  @PostMapping("/refresh")
  public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request) {
    String refreshToken = request.get("refresh_token");
    if (refreshToken == null || refreshToken.isEmpty()) {
      return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Refresh token is required"));
    }

    try {
      Map<String, String> tokens = authService.refreshAccessToken(refreshToken);
      return ResponseEntity.ok(Map.of(
          "status", "success",
          "access_token", tokens.get("access_token"),
          "refresh_token", tokens.get("refresh_token")));
    } catch (Exception e) {
      // Return 401 Unauthorized if the token is invalid or expired
      return ResponseEntity.status(401).body(Map.of("status", "error", "message", e.getMessage()));
    }
  }

  @PostMapping("/logout")
  public ResponseEntity<?> logout(@RequestBody Map<String, String> request) {
    String refreshToken = request.get("refresh_token");
    if (refreshToken != null && !refreshToken.isEmpty()) {
      authService.logout(refreshToken);
    }
    return ResponseEntity.ok(Map.of("status", "success", "message", "Logged out successfully"));
  }
}
