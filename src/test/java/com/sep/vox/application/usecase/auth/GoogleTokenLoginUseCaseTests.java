package com.sep.vox.application.usecase.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.ClientDeviceCommand;
import com.sep.vox.application.port.input.command.GoogleTokenLoginCommand;
import com.sep.vox.application.port.input.command.OAuth2LoginCommand;
import com.sep.vox.application.port.input.usecase.auth.GoogleTokenLoginUseCase;
import com.sep.vox.application.port.input.usecase.auth.OAuth2LoginUseCase;
import com.sep.vox.application.port.output.GoogleIdTokenVerifierPort;
import com.sep.vox.application.response.input.auth.LoginResponse;
import com.sep.vox.application.response.output.VerifiedGoogleIdentity;

/**
 * The Google login path of the native app.
 *
 * <p>This class is deliberately thin, so what is worth pinning is not what it computes but that it
 * does NOT build a session itself: all of that must fall through to {@link OAuth2LoginUseCase}, the
 * very class the web path uses. Having two definitions of "login succeeded" is a mistake that only
 * shows up much later, when a new session rule is applied to web and native is forgotten.
 */
@ExtendWith(MockitoExtension.class)
class GoogleTokenLoginUseCaseTests {

    private static final ClientDeviceCommand DEVICE =
        new ClientDeviceCommand("device-1", "May tinh cua Nhat", "DESKTOP");

    @Mock
    private GoogleIdTokenVerifierPort googleIdTokenVerifierPort;
    @Mock
    private OAuth2LoginUseCase oAuth2LoginUseCase;
    @InjectMocks
    private GoogleTokenLoginUseCase useCase;

    /**
     * The verified identity must pass INTACT into OAuth2LoginCommand -- the same shape that
     * OAuth2AuthenticationSuccessHandler builds for the web path, so both paths create the same kind
     * of session.
     */
    @Test
    void should_hand_the_verified_identity_to_the_shared_login_use_case() {
        givenVerified(new VerifiedGoogleIdentity(
            "google-sub-1", "hs@truong.edu.vn", true, "Tran Le Nhat", "https://lh3.google.com/a"));
        when(oAuth2LoginUseCase.execute(any()))
            .thenReturn(new LoginResponse("access", "refresh", List.of("STUDENT")));

        var result = useCase.execute(command("id-token"));

        var sent = ArgumentCaptor.forClass(OAuth2LoginCommand.class);
        verify(oAuth2LoginUseCase).execute(sent.capture());
        assertThat(sent.getValue().provider()).isEqualTo("google");
        assertThat(sent.getValue().providerUserId()).isEqualTo("google-sub-1");
        assertThat(sent.getValue().email()).isEqualTo("hs@truong.edu.vn");
        assertThat(sent.getValue().device()).isEqualTo(DEVICE);
        assertThat(result.refreshToken()).isEqualTo("refresh");
    }

    /**
     * An unverified email is rejected BEFORE the user lookup.
     *
     * <p>The email is OAuth2LoginUseCase's user lookup key, and a Google account with an unverified
     * email can carry someone else's address. Dropping this check turns "log in with Google" into
     * "log in with any email you can type". It is the same rule CustomOidcUserService applies to web.
     */
    @Test
    void should_refuse_an_unverified_google_email_before_looking_the_user_up() {
        givenVerified(new VerifiedGoogleIdentity(
            "google-sub-2", "gia-mao@truong.edu.vn", false, "Ai Do", null));

        assertThatThrownBy(() -> useCase.execute(command("id-token")))
            .isInstanceOf(UnauthorizedException.class);

        verify(oAuth2LoginUseCase, never()).execute(any());
    }

    /** A broken token stops at the verification gate; no session is built. */
    @Test
    void should_not_open_a_session_when_the_token_fails_verification() {
        when(googleIdTokenVerifierPort.verify(any()))
            .thenThrow(new UnauthorizedException("Đăng nhập Google thất bại"));

        assertThatThrownBy(() -> useCase.execute(command("token-hong")))
            .isInstanceOf(UnauthorizedException.class);

        verify(oAuth2LoginUseCase, never()).execute(any());
    }

    private void givenVerified(VerifiedGoogleIdentity identity) {
        when(googleIdTokenVerifierPort.verify(any())).thenReturn(identity);
    }

    private static GoogleTokenLoginCommand command(String idToken) {
        return new GoogleTokenLoginCommand(idToken, "1.2.3.4", "VoxOralExam/1.0", DEVICE);
    }
}
