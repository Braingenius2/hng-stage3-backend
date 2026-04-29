package com.hng.profile.service;

import com.hng.profile.model.RefreshToken;
import com.hng.profile.model.User;
import com.hng.profile.repository.RefreshTokenRepository;
import com.hng.profile.repository.UserRepository;
import com.hng.profile.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@Service
public class AuthService {

  @Value("${spring.security.oauth2.client.registration.github.client-id}")
  private String clientId;

  @Value("${spring.security.oauth2.client.registration.github.client-secret}")
  private String clientSecret;

  @Value("${admin.github.ids:}")
  private String adminIds;

  private final UserRepository userRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final JwtTokenProvider tokenProvider;
  private final RestTemplate restTemplate = new RestTemplate();

  public AuthService(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository,
      JwtTokenProvider tokenProvider) {
    this.userRepository = userRepository;
    this.refreshTokenRepository = refreshTokenRepository;
    this.tokenProvider = tokenProvider;
  }

  @Transactional
  public Map<String, String> processGithubCallback(String code, String codeVerifier) {
    String githubToken = getGithubAccessToken(code, codeVerifier);
    Map<String, Object> githubUser = getGithubUserProfile(githubToken);
    String githubId = String.valueOf(githubUser.get("id"));

    User user = userRepository.findByGithubId(githubId)
        .orElseGet(() -> {
          User newUser = new User();
          newUser.setGithubId(githubId);
          
          // Determine role: explicit list check OR first user logic
          boolean isAdmin = (adminIds != null && adminIds.contains(githubId)) || userRepository.count() == 0;
          newUser.setRole(isAdmin ? "admin" : "analyst");
          
          return newUser;
        });

    user.setUsername((String) githubUser.get("login"));
    user.setAvatarUrl((String) githubUser.get("avatar_url"));
    user.setEmail((String) githubUser.get("email"));
    user.setLastLoginAt(Instant.now());
    userRepository.save(user);

    // Clear any old sessions for this user (enforcing single-device login)
    refreshTokenRepository.deleteByUser(user);

    String accessToken = tokenProvider.generateAccessToken(githubId, user.getRole());
    String refreshTokenString = tokenProvider.generateRefreshToken(githubId);

    // Save the new refresh token to the database
    RefreshToken refreshToken = new RefreshToken();
    refreshToken.setUser(user);
    refreshToken.setToken(refreshTokenString);
    refreshToken.setExpiryDate(Instant.now().plus(java.time.Duration.ofDays(7)));
    refreshTokenRepository.save(refreshToken);

    return Map.of("access_token", accessToken, "refresh_token", refreshTokenString);
  }

  @Transactional
  public Map<String, String> refreshAccessToken(String oldRefreshToken) {
    // Find token in DB
    RefreshToken tokenEntity = refreshTokenRepository.findByToken(oldRefreshToken)
        .orElseThrow(() -> new RuntimeException("Refresh token not found in database"));

    // Check if it's expired
    if (tokenEntity.getExpiryDate().isBefore(Instant.now())) {
      refreshTokenRepository.delete(tokenEntity);
      throw new RuntimeException("Refresh token has expired");
    }

    // Validate the JWT signature
    if (!tokenProvider.validateToken(oldRefreshToken)) {
      throw new RuntimeException("Invalid refresh token signature");
    }

    User user = tokenEntity.getUser();

    // **CRITICAL SECURITY RULE:** Invalidate the old token immediately!
    refreshTokenRepository.delete(tokenEntity);

    // Issue a new pair
    String newAccessToken = tokenProvider.generateAccessToken(user.getGithubId(), user.getRole());
    String newRefreshTokenString = tokenProvider.generateRefreshToken(user.getGithubId());

    RefreshToken newRefreshToken = new RefreshToken();
    newRefreshToken.setUser(user);
    newRefreshToken.setToken(newRefreshTokenString);
    newRefreshToken.setExpiryDate(Instant.now().plus(java.time.Duration.ofDays(7)));
    refreshTokenRepository.save(newRefreshToken);

    return Map.of("access_token", newAccessToken, "refresh_token", newRefreshTokenString);
  }

  @Transactional
  public void logout(String refreshToken) {
    refreshTokenRepository.findByToken(refreshToken)
        .ifPresent(refreshTokenRepository::delete);
  }

  private String getGithubAccessToken(String code, String codeVerifier) {
    String url = "https://github.com/login/oauth/access_token";
    MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
    body.add("client_id", clientId);
    body.add("client_secret", clientSecret);
    body.add("code", code);
    if (codeVerifier != null && !codeVerifier.isEmpty()) {
      body.add("code_verifier", codeVerifier);
    }

    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
    HttpEntity<?> request = new HttpEntity<>(body, headers);

    ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
    if (response.getBody() == null || !response.getBody().containsKey("access_token")) {
      throw new RuntimeException("Failed to get GitHub access token");
    }
    return (String) response.getBody().get("access_token");
  }

  private Map<String, Object> getGithubUserProfile(String accessToken) {
    String url = "https://api.github.com/user";
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(accessToken);
    HttpEntity<?> request = new HttpEntity<>(headers);

    ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class);
    return response.getBody();
  }
}
