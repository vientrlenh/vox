package com.sep.vox.application.port.input.usecase.auth;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.GoogleTokenLoginCommand;
import com.sep.vox.application.port.input.command.OAuth2LoginCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.GoogleIdTokenVerifierPort;
import com.sep.vox.application.response.input.auth.LoginResponse;

/**
 * Đăng nhập Google cho ứng dụng NATIVE: kiểm chứng ID token rồi giao lại cho
 * {@link OAuth2LoginUseCase}.
 *
 * <p>Cố ý MỎNG. Toàn bộ phần dựng phiên -- tra người dùng đang hoạt động, tạo device session, phát
 * refresh token, gom vai trò -- đã nằm trong {@link OAuth2LoginUseCase} và được đường web dùng hằng
 * ngày. Chép lại ở đây là tự tạo ra hai định nghĩa của "đăng nhập thành công", và cái ít người dùng
 * hơn sẽ là cái âm thầm trôi khỏi cái kia: một bản sửa luật phiên đăng nhập sẽ được áp cho web và
 * quên mất native.
 *
 * <p>Lớp này vì thế chỉ chịu trách nhiệm đúng MỘT việc mà đường web không phải làm: thay thế phần
 * bảo vệ mà Spring Security tự lo trong luồng redirect. Ở đường web, tới lúc
 * {@code OAuth2AuthenticationSuccessHandler} chạy thì token đã được đổi và kiểm bởi chính Spring;
 * ở đây thì không, nên {@link GoogleIdTokenVerifierPort} phải làm đúng phần đó trước.
 */
@Service
public class GoogleTokenLoginUseCase implements IUseCase<GoogleTokenLoginCommand, LoginResponse> {

    /** Nhà cung cấp, ghi vào phiên. Trùng {@code registrationId} của đường web -- cùng một Google. */
    private static final String PROVIDER = "google";

    private final GoogleIdTokenVerifierPort googleIdTokenVerifierPort;
    private final OAuth2LoginUseCase oAuth2LoginUseCase;

    public GoogleTokenLoginUseCase(GoogleIdTokenVerifierPort googleIdTokenVerifierPort,
            OAuth2LoginUseCase oAuth2LoginUseCase) {
        this.googleIdTokenVerifierPort = googleIdTokenVerifierPort;
        this.oAuth2LoginUseCase = oAuth2LoginUseCase;
    }

    /**
     * Email CHƯA xác minh bị từ chối ngay tại đây, trước khi tra người dùng.
     *
     * <p>Cùng luật với {@code CustomOidcUserService} ở đường web, và cùng lý do: email là KHOÁ tra
     * người dùng của {@link OAuth2LoginUseCase}. Một tài khoản Google chưa xác minh email có thể
     * mang địa chỉ của người khác, nên bỏ phép kiểm này là biến "đăng nhập bằng Google" thành "đăng
     * nhập bằng bất kỳ email nào mình gõ ra được".
     *
     * <p>Thông báo lỗi giữ nguyên văn của đường web -- cùng một tình huống thì người dùng phải đọc
     * được cùng một câu, dù họ đang đứng ở trình duyệt hay ở ứng dụng.
     */
    @Override
    @Transactional
    public LoginResponse execute(GoogleTokenLoginCommand input) {
        var identity = googleIdTokenVerifierPort.verify(input.idToken());

        if (!identity.emailVerified()) {
            throw new UnauthorizedException("Người dùng chưa được xác thực để đăng nhập");
        }

        return oAuth2LoginUseCase.execute(new OAuth2LoginCommand(
            PROVIDER,
            identity.subject(),
            identity.email(),
            identity.emailVerified(),
            identity.fullName(),
            identity.avatarUrl(),
            input.ipAddress(),
            input.userAgent(),
            input.device()
        ));
    }
}
