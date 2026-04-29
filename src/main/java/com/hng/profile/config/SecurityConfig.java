package com.hng.profile.config;

import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import com.hng.profile.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import com.hng.profile.security.RateLimitFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final ApiVersionFilter apiVersionFilter;
  private final RateLimitFilter rateLimitFilter;

  public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, 
                        ApiVersionFilter apiVersionFilter,
                        RateLimitFilter rateLimitFilter) {
    this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    this.apiVersionFilter = apiVersionFilter;
    this.rateLimitFilter = rateLimitFilter;
  }


  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(ex -> ex
            .authenticationEntryPoint((request, response, authException) -> {
                response.setStatus(401);
                response.setContentType("application/json");
                response.getWriter().write("{\"status\":\"error\",\"message\":\"Unauthorized\"}");
            })
            .accessDeniedHandler((request, response, accessDeniedException) -> {
                response.setStatus(403);
                response.setContentType("application/json");
                response.getWriter().write("{\"status\":\"error\",\"message\":\"Forbidden\"}");
            })
        )
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(new AntPathRequestMatcher("/auth/**")).permitAll()
            .requestMatchers(new AntPathRequestMatcher("/api/**")).authenticated()
            .anyRequest().permitAll())
        .addFilterBefore(apiVersionFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }


  @Bean
  public org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource() {
    org.springframework.web.cors.CorsConfiguration configuration = new org.springframework.web.cors.CorsConfiguration();
    configuration.setAllowedOriginPatterns(java.util.List.of("*"));
    configuration.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(java.util.List.of("Authorization", "Content-Type", "X-API-Version"));
    configuration.setAllowCredentials(true);
    configuration.setExposedHeaders(java.util.List.of("X-Rate-Limit-Remaining", "X-Rate-Limit-Retry-After-Seconds"));
    
    org.springframework.web.cors.UrlBasedCorsConfigurationSource source = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}
