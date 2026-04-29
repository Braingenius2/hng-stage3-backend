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

  // The CLI or Web Portal will call this after GitHub redirects back to them.
  @PostMapping("/github/callback")
  public ResponseEntity<Map<String, String>> githubCallback(@RequestBody Map<String, String> request) {

    String code = request.get("code");
    String codeVerifier = request.get("code_verifier");

    if (code == null || code.isEmpty()) {
      return ResponseEntity.badRequest().body(Map.of("error", "Authorization code is required"));
    }

    Map<String, String> tokens = authService.processGithubCallback(code, codeVerifier);

    return ResponseEntity.ok(tokens);
  }
}
