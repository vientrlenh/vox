package com.sep.vox.infrastructure.filter;

import java.io.IOException;

import org.springframework.security.authentication.DisabledException;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j 
@RequiredArgsConstructor 
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final AuthTokenPort authTokenPort;
    private final UserDetailsService userDetailsService;

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final int TOKEN_BEGIN_INDEX = 7;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            log.info("Incoming request: {}", request.getRequestURI());
            var jwtToken = getTokenFromRequest(request);
            if (jwtToken != null) {
                var email = authTokenPort.extractEmail(jwtToken);
                var userDetails = userDetailsService.loadUserByUsername(email);
                if (!userDetails.isEnabled()) {
                    throw new DisabledException("Trạng thái người dùng không hợp lệ");
                }
                var auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        } catch (Exception e) {
            SecurityContextHolder.clearContext();
            log.error("User authentication set failed: {}", e.getMessage());
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
