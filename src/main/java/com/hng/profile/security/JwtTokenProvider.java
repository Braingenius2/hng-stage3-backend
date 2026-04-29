package com.hng.profile.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;

@Component
public class JwtTokenProvider {

  @Value("${jwt.secret:ThisIsASecretKeyForInsightaLabsThatIsVeryLong}")
  private String jwtSecret;

  private static final long ACCESS_TOKEN_EXPIRATION_MS = 3 * 60 * 1000; // 3 minutes
  private static final long REFRESH_TOKEN_EXPIRATION_MS = 5 * 60 * 1000; // 5 minutes

  private SecretKey getSigningKey() {
    return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
  }

  public String generateAccessToken(String githubId, String role) {
    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + ACCESS_TOKEN_EXPIRATION_MS);

    return Jwts.builder()
        .subject(githubId) // The main identifier for this token
        .claim("role", role) // We embed the role so we don't have to query the DB every time!
        .issuedAt(now)
        .expiration(expiryDate)
        .signWith(getSigningKey())
        .compact();
  }

  public String generateRefreshToken(String githubId) {
    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + REFRESH_TOKEN_EXPIRATION_MS);

    // Refresh tokens don't need roles, they are just used to get a new Access
    // token.
    return Jwts.builder()
        .subject(githubId)
        .issuedAt(now)
        .expiration(expiryDate)
        .signWith(getSigningKey())
        .compact();
  }

  public boolean validateToken(String token) {
    try {
      Jwts.parser()
          .verifyWith(getSigningKey())
          .build()
          .parseSignedClaims(token);
      return true;
    } catch (Exception ex) {
      // Token is expired, tampered with, or invalid
      return false;
    }
  }

  public String getGithubIdFromToken(String token) {
    Claims claims = Jwts.parser()
        .verifyWith(getSigningKey())
        .build()
        .parseSignedClaims(token)
        .getPayload();
    return claims.getSubject();
  }

  public String getRoleFromToken(String token) {
    Claims claims = Jwts.parser()
        .verifyWith(getSigningKey())
        .build()
        .parseSignedClaims(token)
        .getPayload();
    return claims.get("role", String.class);
  }
}
