package com.sep.vox.interfaces.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.test.util.ReflectionTestUtils;

import com.sep.vox.application.port.input.command.auth.OAuth2LoginCommand;
import com.sep.vox.application.port.input.usecase.auth.OAuth2LoginUseCase;
import com.sep.vox.interfaces.shared.HttpCookieProvider;
import com.sep.vox.application.response.input.auth.LoginResponse;
import com.sep.vox.domain.model.devicesession.SessionPlatform;

class OAuth2AuthenticationSuccessHandlerTests {

    private OAuth2LoginUseCase oAuth2LoginUseCase;
    private OAuth2AuthenticationSuccessHandler handler;
    private HttpCookieProvider httpCookieProvider;

    @BeforeEach
    void setUp() {
        oAuth2LoginUseCase = mock(OAuth2LoginUseCase.class);
        httpCookieProvider = mock(HttpCookieProvider.class);
        handler = new OAuth2AuthenticationSuccessHandler(oAuth2LoginUseCase, httpCookieProvider);
        ReflectionTestUtils.setField(handler, "returnUrl", "http://localhost:5173/oauth2-callback");
    }

    @Test
    void onAuthenticationSuccess_should_issue_tokens_set_cookie_and_redirect() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.10");
        request.addHeader("User-Agent", "JUnit User Agent");
        HttpSession session = request.getSession();
        session.setAttribute("oauth2_device_id", "device-1");
        session.setAttribute("oauth2_device_name", "Chrome on Windows");
        session.setAttribute("oauth2_platform", SessionPlatform.WEB);

        MockHttpServletResponse response = new MockHttpServletResponse();
        OAuth2AuthenticationToken authentication = googleAuthentication();
        when(oAuth2LoginUseCase.execute(any(OAuth2LoginCommand.class)))
            .thenReturn(new LoginResponse("access-token", "refresh-token", List.of("STUDENT")));

        handler.onAuthenticationSuccess(request, response, authentication);

        ArgumentCaptor<OAuth2LoginCommand> commandCaptor = ArgumentCaptor.forClass(OAuth2LoginCommand.class);
        verify(oAuth2LoginUseCase).execute(commandCaptor.capture());
        OAuth2LoginCommand command = commandCaptor.getValue();
        assertThat(command.provider()).isEqualTo("google");
        assertThat(command.providerUserId()).isEqualTo("google-user-id");
        assertThat(command.email()).isEqualTo("student@example.com");
        assertThat(command.emailVerified()).isTrue();
        assertThat(command.fullName()).isEqualTo("Test Student");
        assertThat(command.avatarUrl()).isEqualTo("https://example.com/avatar.png");
        assertThat(command.ipAddress()).isEqualTo("203.0.113.10");
        assertThat(command.userAgent()).isEqualTo("JUnit User Agent");
        assertThat(command.device().deviceId()).isEqualTo("device-1");
        assertThat(command.device().deviceName()).isEqualTo("Chrome on Windows");
        assertThat(command.device().platform()).isEqualTo(SessionPlatform.WEB);

        verify(httpCookieProvider).setCookie(response, "refresh_token", "refresh-token", 259200L);
        assertThat(response.getRedirectedUrl())
            .isEqualTo("http://localhost:5173/oauth2-callback?token=access-token");
        assertThrows(IllegalStateException.class, () -> session.getAttribute("oauth2_device_id"));
    }

    @Test
    void onAuthenticationSuccess_should_use_forwarded_ip_when_present() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "198.51.100.20, 203.0.113.10");
        request.addHeader("User-Agent", "JUnit User Agent");
        HttpSession session = request.getSession();
        session.setAttribute("oauth2_device_id", "device-1");
        session.setAttribute("oauth2_device_name", "Chrome on Windows");
        session.setAttribute("oauth2_platform", SessionPlatform.WEB);

        MockHttpServletResponse response = new MockHttpServletResponse();
        when(oAuth2LoginUseCase.execute(any(OAuth2LoginCommand.class)))
            .thenReturn(new LoginResponse("access-token", "refresh-token", List.of("STUDENT")));

        handler.onAuthenticationSuccess(request, response, googleAuthentication());

        ArgumentCaptor<OAuth2LoginCommand> commandCaptor = ArgumentCaptor.forClass(OAuth2LoginCommand.class);
        verify(oAuth2LoginUseCase).execute(commandCaptor.capture());
        assertThat(commandCaptor.getValue().ipAddress()).isEqualTo("198.51.100.20");
    }

    @Test
    void onAuthenticationSuccess_should_throw_unauthorized_when_session_is_missing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, googleAuthentication());

        verifyNoInteractions(oAuth2LoginUseCase);
    }

    @Test
    void onAuthenticationSuccess_should_not_set_cookie_when_login_use_case_throws() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        HttpSession session = request.getSession();
        session.setAttribute("oauth2_device_id", "device-1");
        session.setAttribute("oauth2_device_name", "Chrome on Windows");
        session.setAttribute("oauth2_platform", SessionPlatform.WEB);
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(oAuth2LoginUseCase.execute(any(OAuth2LoginCommand.class)))
            .thenThrow(new IllegalStateException("login failed"));

        handler.onAuthenticationSuccess(request, response, googleAuthentication());

        verifyNoInteractions(httpCookieProvider);
        assertThat(response.getRedirectedUrl())
            .isEqualTo("http://localhost:5173/oauth2-callback?error=login_failed");
    }

    private static OAuth2AuthenticationToken googleAuthentication() {
        Map<String, Object> attributes = Map.<String, Object>of(
            "sub", "google-user-id",
            "email", "student@example.com",
            "email_verified", true,
            "name", "Test Student",
            "picture", "https://example.com/avatar.png"
        );
        DefaultOAuth2User principal = new DefaultOAuth2User(List.of(), attributes, "sub");
        return new OAuth2AuthenticationToken(principal, principal.getAuthorities(), "google");
    }
}
