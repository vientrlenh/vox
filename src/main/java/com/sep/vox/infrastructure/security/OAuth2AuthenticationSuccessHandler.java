package com.sep.vox.infrastructure.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.OAuth2LoginCommand;
import com.sep.vox.application.port.input.usecase.auth.OAuth2LoginUseCase;
import com.sep.vox.application.response.input.auth.LoginResponse;
import com.sep.vox.domain.model.platform.Platform;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j 
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final OAuth2LoginUseCase oAuth2LoginUseCase;
    private final String returnUrl;

    public OAuth2AuthenticationSuccessHandler(
        OAuth2LoginUseCase oAuth2LoginUseCase, 
        @Value("${app.frontend.oauth2-url}") String returnUrl
    ) {
        this.oAuth2LoginUseCase = oAuth2LoginUseCase;
        this.returnUrl = returnUrl;
    }

    

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        try {
            OAuth2AuthenticationToken oauth2Token = (OAuth2AuthenticationToken) authentication;
            String provider = oauth2Token.getAuthorizedClientRegistrationId();
            OAuth2User user = oauth2Token.getPrincipal();

            HttpSession session = request.getSession(false);
            if (session == null) {
                throw new UnauthorizedException("Login failed");
            }
            String deviceId = (String) session.getAttribute("oauth2_device_id");
            String deviceName = (String) session.getAttribute("oauth2_device_name");
            Platform platform = (Platform) session.getAttribute("oauth2_platform");

            OAuth2LoginCommand command = new OAuth2LoginCommand(
                provider, 
                user.getAttribute("sub"), 
                user.getAttribute("email"), 
                user.getAttribute("email_verified"), 
                user.getAttribute("name"), 
                user.getAttribute("picture"), 
                deviceId, 
                deviceName, 
                platform, 
                request, 
                response
            );

            LoginResponse data = oAuth2LoginUseCase.execute(command);
            clearSession(session);
            redirect(response, returnUrl, "token", data.accessToken());
        } catch (IllegalArgumentException e) {
            redirect(response, returnUrl, "error", "unsupported_platform");
        } catch (Exception e) {
            log.error("OAuth2 login failed: ", e);
            redirect(response, returnUrl, "error", "login_failed");
        } 
    }

    private void clearSession(HttpSession session) {
        session.removeAttribute("oauth2_device_id");
        session.removeAttribute("oauth2_device_name");
        session.removeAttribute("oauth2_platform");
        session.invalidate();
    }

    private void redirect(HttpServletResponse response, String baseUrl, String param, String value) throws IOException {
        String url = UriComponentsBuilder.fromUriString(baseUrl)
            .queryParam(param, value)
            .encode(StandardCharsets.UTF_8)
            .build()
            .toUriString();
        response.sendRedirect(url);
    }
    
}
