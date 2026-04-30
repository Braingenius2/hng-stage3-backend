package com.hng.profile.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class ApiVersionFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    // We only care about checking the header for our API and Auth endpoints
    String path = request.getRequestURI();
    if ((path.startsWith("/api") || path.startsWith("/auth")) && !path.equals("/auth/github")) {

      String apiVersion = request.getHeader("X-API-Version");

      if (apiVersion == null || !apiVersion.equals("1")) {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType("application/json");
        response.getWriter().write("""
                {
                    "status": "error",
                    "code": "unsupported_api_version",
                    "message": "Only API version 1 is supported"
                }
            """);
        return; // Stop processing and send the error immediately!
      }
    }

    // If the header is correct (or if it's not an API path), continue the chain
    filterChain.doFilter(request, response);
  }
}
