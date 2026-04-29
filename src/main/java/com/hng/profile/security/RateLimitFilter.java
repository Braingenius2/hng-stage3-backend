package com.hng.profile.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> authBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> apiBuckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        // Fallback to IP address if the user is unauthenticated
        String clientId = request.getRemoteAddr();
        
        // If they sent an Authorization header, we can use their token as the unique ID!
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            clientId = authHeader.substring(7);
        }

        // 10 req/min for auth endpoints
        if (path.startsWith("/auth/")) {
            Bucket bucket = authBuckets.computeIfAbsent(clientId, this::createNewAuthBucket);
            if (!bucket.tryConsume(1)) {
                sendTooManyRequestsError(response);
                return;
            }
        } 
        // 60 req/min for core APIs
        else if (path.startsWith("/api/")) {
            Bucket bucket = apiBuckets.computeIfAbsent(clientId, this::createNewApiBucket);
            if (!bucket.tryConsume(1)) {
                sendTooManyRequestsError(response);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private Bucket createNewAuthBucket(String key) {
        Bandwidth limit = Bandwidth.builder().capacity(10).refillGreedy(10, Duration.ofMinutes(1)).build();
        return Bucket.builder().addLimit(limit).build();
    }

    private Bucket createNewApiBucket(String key) {
        Bandwidth limit = Bandwidth.builder().capacity(60).refillGreedy(60, Duration.ofMinutes(1)).build();
        return Bucket.builder().addLimit(limit).build();
    }

    private void sendTooManyRequestsError(HttpServletResponse response) throws IOException {
        response.setStatus(429);
        response.setContentType("application/json");
        response.getWriter().write("""
            {
                "status": "error",
                "message": "Too many requests. Please try again later."
            }
        """);
    }
}
