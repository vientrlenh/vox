package com.sep.vox.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

/**
 * Minimal security for {@link com.sep.vox.support.RestControllerTest} slices. It is NOT the production chain
 * ({@code SecurityConfig} needs the JWT filter, OAuth2 handlers and CORS properties), so these slices
 * do not cover CSRF, CORS or the JWT filter.
 *
 * <p>It does enforce what controller tests depend on: {@code /api/v1/auth/**} is public, every other
 * request needs an authenticated user and gets 401 otherwise, and {@code @PreAuthorize} is evaluated
 * so that a wrong role is answered with 403 through the real {@code GlobalExceptionHandler}.
 */
@TestConfiguration(proxyBeanMethods = false)
@EnableWebSecurity
@EnableMethodSecurity
public class WebMvcTestSecurityConfiguration {

    @Bean
    SecurityFilterChain webMvcTestSecurityFilterChain(HttpSecurity http) {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(requests -> requests
                .requestMatchers("/api/v1/auth/**").permitAll()
                .anyRequest().authenticated())
            .exceptionHandling(handling -> handling
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));
        return http.build();
    }
}
