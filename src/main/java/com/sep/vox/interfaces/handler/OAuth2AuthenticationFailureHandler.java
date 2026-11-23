package com.sep.vox.interfaces.handler;

import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Slf4j
public class OAuth2AuthenticationFailureHandler implements AuthenticationFailureHandler {

    private static final Set<String> KNOWN_CODES = Set.of("email_not_verified", "user_not_found", "unsupported_platform");

    @Value("${app.frontend.oauth2-url}")
    private String returnUrl;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) throws IOException, ServletException {
        clearSession(request);

        String message = resolveMessage(exception);
        log.warn("OAuth2 login failed: {}", exception.getMessage());

        String url = UriComponentsBuilder.fromUriString(returnUrl)
                .queryParam("error", message)
                .encode(StandardCharsets.UTF_8)
                .build()
                .toUriString();
        response.sendRedirect(url);
    }

    private String resolveMessage(AuthenticationException e) {
        if (e instanceof OAuth2AuthenticationException oae) {
            OAuth2Error error = oae.getError();
            if (KNOWN_CODES.contains(error.getErrorCode())) {
                return error.getErrorCode();
            }
        }
        return "login_failed";
    }

    private void clearSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute("oauth2_device_id");
            session.removeAttribute("oauth2_device_name");
            session.removeAttribute("oauth2_platform");
            session.invalidate();
        }
    }
    
}
