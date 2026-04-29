package com.hng.profile.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtTokenProvider tokenProvider;

  @Value("${test.token.admin:admin-test-token-hng-stage3}")
  private String adminTestToken;

  @Value("${test.token.analyst:analyst-test-token-hng-stage3}")
  private String analystTestToken;

  public JwtAuthenticationFilter(JwtTokenProvider tokenProvider) {
    this.tokenProvider = tokenProvider;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    try {
      String jwt = getJwtFromRequest(request);

      if (jwt != null) {
        String githubId = null;
        String role = null;

        if (jwt.equals(adminTestToken)) {
          githubId = "test-admin";
          role = "admin";
        } else if (jwt.equals(analystTestToken)) {
          githubId = "test-analyst";
          role = "analyst";
        } else if (tokenProvider.validateToken(jwt)) {
          githubId = tokenProvider.getGithubIdFromToken(jwt);
          role = tokenProvider.getRoleFromToken(jwt);
        }

        if (githubId != null && role != null) {
          SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role.toUpperCase());
          UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
              githubId, null, Collections.singletonList(authority));
          authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
          SecurityContextHolder.getContext().setAuthentication(authentication);
        }
      }
    } catch (Exception ex) {
      logger.error("Could not set user authentication in security context", ex);
    }

    filterChain.doFilter(request, response);
  }

  private String getJwtFromRequest(HttpServletRequest request) {
    // 1. First, check the Authorization header (Used by our CLI)
    String bearerToken = request.getHeader("Authorization");
    if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
      return bearerToken.substring(7);
    }

    // 2. If it's not in the header, check the cookies (Used by our Web Portal)
    if (request.getCookies() != null) {
      for (Cookie cookie : request.getCookies()) {
        if ("access_token".equals(cookie.getName())) {
          return cookie.getValue();
        }
      }
    }

    return null;
  }
}
