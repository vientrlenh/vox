package com.sep.vox.infrastructure.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter.HeaderValue;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.sep.vox.application.port.output.PasswordEncoderPort;
import com.sep.vox.infrastructure.filter.JwtAuthenticationFilter;
import com.sep.vox.infrastructure.security.Argon2PasswordEncodeProvider;
import com.sep.vox.infrastructure.security.CustomOidcUserService;
import com.sep.vox.infrastructure.security.OAuth2AuthenticationFailureHandler;
import com.sep.vox.infrastructure.security.OAuth2AuthenticationSuccessHandler;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    private final OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;
    private final CustomOidcUserService customOidcUserService;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler, OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler, CustomOidcUserService customOidcUserService) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.oAuth2AuthenticationSuccessHandler = oAuth2AuthenticationSuccessHandler;
        this.oAuth2AuthenticationFailureHandler = oAuth2AuthenticationFailureHandler;
        this.customOidcUserService = customOidcUserService;
    }
    
    private static final long HSTS_MAX_AGE_IN_SECONDS = 31536000;
    private static final boolean HSTS_INCLUDE_SUB_DOMAINS = true;
    private static final boolean HSTS_PRELOAD = true;

    private static final String[] PERMITTED_PATTERNS = {
        "/swagger-ui/**",
        "/v3/api-spec/**",
        "/error",
        "/api/v1/status",
        "/api/v1/auth/**",
        "/graphql",
        "/graphiql/**"
    };

    private static final List<String> CORS_ALLOWED_PATTERNS = List.of(
        "http://localhost:5173"
    );
    private static final List<String> CORS_ALLOWED_METHODS = List.of(
        HttpMethod.GET.name(),
        HttpMethod.POST.name(),
        HttpMethod.PUT.name(),
        HttpMethod.DELETE.name(),
        HttpMethod.PATCH.name(),
        HttpMethod.OPTIONS.name()
    );

    private static final boolean CORS_ALLOW_CREDENTIALS = true;
    private static final List<String> CORS_ALLOWED_HEADERS = List.of("*");

    private static final int ARGON2_SALT_LENGTH = 16;
    private static final int ARGON2_HASH_LENGTH = 32;
    private static final int ARGON2_PARALLELISM = 1;
    private static final int ARGON2_MEMORY = 1 << 12;
    private static final int ARGON2_ITERATION = 3;

    
    @Bean
    public SecurityFilterChain configure(HttpSecurity http) {
        http
            .csrf(csrf -> csrf.disable())
            .formLogin(fl -> fl.disable())
            .httpBasic(hb -> hb.disable())
            .cors(cors -> cors
                .configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin())
                .xssProtection(xss -> xss
                    .headerValue(HeaderValue.ENABLED_MODE_BLOCK))
                .httpStrictTransportSecurity(hsts -> hsts
                    .maxAgeInSeconds(HSTS_MAX_AGE_IN_SECONDS)
                    .includeSubDomains(HSTS_INCLUDE_SUB_DOMAINS)
                    .preload(HSTS_PRELOAD))
                .contentTypeOptions(_ -> {}))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(PERMITTED_PATTERNS)
                .permitAll()
                .anyRequest()
                .authenticated())
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(u -> u.oidcUserService(customOidcUserService))
                .successHandler(oAuth2AuthenticationSuccessHandler)
                .failureHandler(oAuth2AuthenticationFailureHandler)
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        var config = new CorsConfiguration();
        config.setAllowedOriginPatterns(CORS_ALLOWED_PATTERNS);
        config.setAllowedMethods(CORS_ALLOWED_METHODS);
        config.setAllowCredentials(CORS_ALLOW_CREDENTIALS);
        config.setAllowedHeaders(CORS_ALLOWED_HEADERS);

        var urlBasedSource = new UrlBasedCorsConfigurationSource();
        urlBasedSource.registerCorsConfiguration("/**", config);
        return urlBasedSource;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new Argon2PasswordEncoder(
            ARGON2_SALT_LENGTH, 
            ARGON2_HASH_LENGTH, 
            ARGON2_PARALLELISM, 
            ARGON2_MEMORY, 
            ARGON2_ITERATION
        );
    }


    @Bean
    public PasswordEncoderPort passwordEncoderPort() {
        return new Argon2PasswordEncodeProvider(passwordEncoder());
    }
}
