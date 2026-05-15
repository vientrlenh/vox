package com.sep.vox.infrastructure.security;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.sep.vox.application.port.output.AuthTokenPort;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final AuthTokenPort authTokenPort;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(AuthTokenPort authTokenPort, UserDetailsService userDetailsService) {
        this.authTokenPort = authTokenPort;
        this.userDetailsService = userDetailsService;
    }

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final int TOKEN_BEGIN_INDEX = 7;
    private static final Logger LOGGER = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            LOGGER.info("Incoming request: {}", request.getRequestURI());
            var jwtToken = getTokenFromRequest(request);
            if (jwtToken != null) {
                var email = authTokenPort.getEmailFromToken(jwtToken, "ACCESS");
                var userDetails = userDetailsService.loadUserByUsername(email);
                var auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        } catch (Exception e) {
            LOGGER.error("User authentication set failed: {}", e.getMessage());
        }
        filterChain.doFilter(request, response);
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        var token = request.getHeader(AUTHORIZATION_HEADER);
        if (token != null && token.startsWith(BEARER_PREFIX)) {
            return token.substring(TOKEN_BEGIN_INDEX);
        }
        return null;
    }
    
}
