package com.sep.vox.infrastructure.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.ClientDeviceCommand;
import com.sep.vox.application.port.input.command.OAuth2LoginCommand;
import com.sep.vox.application.port.input.usecase.auth.OAuth2LoginUseCase;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Component
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final OAuth2LoginUseCase oAuth2LoginUseCase;

    public OAuth2AuthenticationSuccessHandler(OAuth2LoginUseCase oAuth2LoginUseCase) {
        this.oAuth2LoginUseCase = oAuth2LoginUseCase;
    }

    private static final long REFRESH_TOKEN_COOKIE_TTL_SECONDS = 259200L;
    private static final Logger LOGGER = LoggerFactory.getLogger(OAuth2AuthenticationSuccessHandler.class);

    @Value("${app.frontend.oauth2-url}")
    private String returnUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        try {
            var oauth2Token = (OAuth2AuthenticationToken) authentication;
            var provider = oauth2Token.getAuthorizedClientRegistrationId();
            var user = oauth2Token.getPrincipal();

            var session = request.getSession(false);
            if (session == null) {
                throw new UnauthorizedException("Đăng nhập thất bại");
            }
            var deviceId = (String) session.getAttribute("oauth2_device_id");
            var deviceName = (String) session.getAttribute("oauth2_device_name");
            var platform = (String) session.getAttribute("oauth2_platform");
            var device = new ClientDeviceCommand(
                deviceId, 
                deviceName, 
                platform,
                null
            );

            var command = new OAuth2LoginCommand(
                provider, 
                user.getAttribute("sub"), 
                user.getAttribute("email"), 
                user.getAttribute("email_verified"), 
                user.getAttribute("name"), 
                user.getAttribute("picture"), 
                ipAddress(request), 
                request.getHeader("User-Agent"), 
                device
            );

            var data = oAuth2LoginUseCase.execute(command);
            setRefreshTokenCookie(response, data.refreshToken(), REFRESH_TOKEN_COOKIE_TTL_SECONDS);
            clearSession(session);
            redirect(response, returnUrl, "token", data.accessToken());
        } catch (IllegalArgumentException e) {
            redirect(response, returnUrl, "error", "unsupported_platform");
        } catch (Exception e) {
            LOGGER.error("OAuth2 login failed: ", e);
            redirect(response, returnUrl, "error", "login_failed");
        }
        
    }

    private String ipAddress(HttpServletRequest request) {
        var forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank() && !"unknown".equalsIgnoreCase(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }

        var realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank() && !"unknown".equalsIgnoreCase(realIp)) {
            return realIp.trim();
        }

        return request.getRemoteAddr();
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String value, long ttl) {
        var cookie = ResponseCookie.from("refresh_token", value)
            .httpOnly(true) 
            .secure(false)
            .path("/")
            .maxAge(ttl)
            .sameSite("Lax")
            .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
    

    private void clearSession(HttpSession session) {
        session.removeAttribute("oauth2_device_id");
        session.removeAttribute("oauth2_device_name");
        session.removeAttribute("oauth2_platform");
        session.invalidate();
    }

    private void redirect(HttpServletResponse response, String baseUrl, String param, String value) throws IOException {
        var url = UriComponentsBuilder.fromUriString(baseUrl)
            .queryParam(param, value)
            .encode(StandardCharsets.UTF_8)
            .build()
            .toUriString();
        response.sendRedirect(url);
    }
    
}
