package com.hng.profile.service;

import com.hng.profile.model.User;
import com.hng.profile.repository.UserRepository;
import com.hng.profile.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;

@Service
public class AuthService {

  @Value("${github.client.id:dummy_id}")
  private String clientId;

  @Value("${github.client.secret:dummy_secret}")
  private String clientSecret;

  private final UserRepository userRepository;
  private final JwtTokenProvider tokenProvider;
  private final RestTemplate restTemplate = new RestTemplate();

  public AuthService(UserRepository userRepository, JwtTokenProvider tokenProvider) {
    this.userRepository = userRepository;
    this.tokenProvider = tokenProvider;
  }

  public Map<String, String> processGithubCallback(String code, String codeVerifier) {
    // 1. Exchange the code + PKCE verifier for a GitHub Access Token
    String githubToken = getGithubAccessToken(code, codeVerifier);

    // 2. Fetch the user's profile from GitHub
    Map<String, Object> githubUser = getGithubUserProfile(githubToken);
    String githubId = String.valueOf(githubUser.get("id"));

    // 3. Create or update the user in our database
    User user = userRepository.findByGithubId(githubId)
        .orElseGet(() -> {
          User newUser = new User();
          newUser.setGithubId(githubId);
          // First user to log in gets admin, everyone else is an analyst!
          newUser.setRole(userRepository.count() == 0 ? "admin" : "analyst");
          return newUser;
        });

    user.setUsername((String) githubUser.get("login"));
    user.setAvatarUrl((String) githubUser.get("avatar_url"));
    user.setEmail((String) githubUser.get("email"));
    user.setLastLoginAt(java.time.Instant.now());

    userRepository.save(user);

    // 4. Generate our own Insighta JWTs!
    String accessToken = tokenProvider.generateAccessToken(githubId, user.getRole());
    String refreshToken = tokenProvider.generateRefreshToken(githubId);

    return Map.of(
        "access_token", accessToken,
        "refresh_token", refreshToken);
  }

  private String getGithubAccessToken(String code, String codeVerifier) {
    String url = "https://github.com/login/oauth/access_token";

    MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
    body.add("client_id", clientId);
    body.add("client_secret", clientSecret);
    body.add("code", code);
    if (codeVerifier != null && !codeVerifier.isEmpty()) {
      body.add("code_verifier", codeVerifier); // PKCE support
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
