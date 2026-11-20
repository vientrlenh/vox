package com.sep.vox.application.usecase.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

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
 * Đường đăng nhập Google của ứng dụng native.
 *
 * <p>Lớp này cố ý mỏng, nên thứ đáng chốt không phải nó tính ra gì mà là nó KHÔNG tự dựng phiên:
 * toàn bộ việc đó phải rơi về {@link OAuth2LoginUseCase}, đúng lớp mà đường web đang dùng. Hai định
 * nghĩa của "đăng nhập thành công" là một sai lầm chỉ lộ ra rất lâu về sau, khi một luật phiên mới
 * được áp cho web và quên mất native.
 */
class GoogleTokenLoginUseCaseTests {

    private static final ClientDeviceCommand DEVICE =
        new ClientDeviceCommand("device-1", "May tinh cua Nhat", "DESKTOP");

    private GoogleIdTokenVerifierPort googleIdTokenVerifierPort;
    private OAuth2LoginUseCase oAuth2LoginUseCase;
    private GoogleTokenLoginUseCase useCase;

    @BeforeEach
    void setUp() {
        googleIdTokenVerifierPort = mock(GoogleIdTokenVerifierPort.class);
        oAuth2LoginUseCase = mock(OAuth2LoginUseCase.class);
        useCase = new GoogleTokenLoginUseCase(googleIdTokenVerifierPort, oAuth2LoginUseCase);
    }

    /**
     * Danh tính đã kiểm chứng phải đi NGUYÊN VẸN sang OAuth2LoginCommand -- cùng hình dạng mà
     * OAuth2AuthenticationSuccessHandler dựng cho đường web, để hai đường tạo ra cùng một loại phiên.
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
     * Email chưa xác minh bị chặn TRƯỚC khi tra người dùng.
     *
     * <p>Email là khoá tra người dùng của OAuth2LoginUseCase, và một tài khoản Google chưa xác minh
     * email có thể mang địa chỉ của người khác. Bỏ phép kiểm này là biến "đăng nhập bằng Google"
     * thành "đăng nhập bằng bất kỳ email nào gõ ra được". Cùng luật CustomOidcUserService áp cho web.
     */
    @Test
    void should_refuse_an_unverified_google_email_before_looking_the_user_up() {
        givenVerified(new VerifiedGoogleIdentity(
            "google-sub-2", "gia-mao@truong.edu.vn", false, "Ai Do", null));

        assertThatThrownBy(() -> useCase.execute(command("id-token")))
            .isInstanceOf(UnauthorizedException.class);

        verify(oAuth2LoginUseCase, never()).execute(any());
    }

    /** Token hỏng thì dừng ở cửa kiểm, không có phiên nào được dựng. */
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
