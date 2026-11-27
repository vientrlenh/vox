package com.sep.vox.infrastructure.filter;

import java.io.IOException;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Authenticates the internal Python->Java endpoints (/internal/practice-sessions/**) with a shared secret
 * -- not the user JWT, because the caller is Python, not a logged-in student. The paths
 * /internal/practice-selection and /internal/practice-generation (which already existed) are NOT protected by this
 * filter -- it only applies to the new paths, leaving the behavior of the two old paths unchanged.
 */
@Component
@Slf4j
public class PracticeInternalSecretFilter extends OncePerRequestFilter {

    private static final String SECRET_HEADER = "X-Internal-Secret";
    private static final String[] PROTECTED_PREFIXES = {
        "/internal/practice-sessions/",
        // The assessment path also moves to Python uploading the turn's audio itself, instead of WPF uploading and then
        // calling POST /turns/archive. Same shared-secret mechanism, because the caller is still Python.
        "/internal/assessment-turns/",
    };

    private final String expectedSecret;

    public PracticeInternalSecretFilter(
            @Value("${practice.internal.secret:}") String expectedSecret) {
        this.expectedSecret = expectedSecret;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String uri = request.getRequestURI();
        boolean protectedPath = false;
        for (String prefix : PROTECTED_PREFIXES) {
            if (uri.startsWith(prefix)) {
                protectedPath = true;
                break;
            }
        }
        if (!protectedPath) {
            filterChain.doFilter(request, response);
            return;
        }
        String provided = request.getHeader(SECRET_HEADER);
        if (expectedSecret.isBlank() || provided == null || !expectedSecret.equals(provided)) {
            log.warn("Rejected a call to the internal practice endpoint: {}", request.getRequestURI());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        filterChain.doFilter(request, response);
    }
}
