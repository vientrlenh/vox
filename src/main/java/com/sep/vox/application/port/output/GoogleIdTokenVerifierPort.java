package com.sep.vox.application.port.output;

import com.sep.vox.application.response.output.VerifiedGoogleIdentity;

/**
 * Kiểm chứng ID token do Google cấp cho ứng dụng NATIVE (Flutter, WPF).
 *
 * <p>Vì sao cần đường này bên cạnh luồng redirect của web: luồng web do Spring Security lo trọn gói
 * -- nó tự đổi code lấy token, tự kiểm chữ ký, rồi kết thúc bằng một lần chuyển hướng về ĐÚNG MỘT
 * URL cấu hình sẵn ({@code app.frontend.oauth2-url}). Ứng dụng native không có chỗ nào nhận cú
 * chuyển hướng đó, nên chúng tự lấy ID token bằng SDK/PKCE ở phía máy người dùng rồi nộp lên đây.
 *
 * <p><b>Cửa này thay thế toàn bộ phần bảo vệ mà Spring Security làm hộ ở đường kia</b>, nên phần
 * cài đặt phải kiểm đủ bốn thứ, thiếu cái nào cũng là một lỗ đăng nhập thật:
 * <ul>
 *   <li>chữ ký, đối chiếu với bộ khoá công khai của Google;</li>
 *   <li>{@code iss} thuộc về Google;</li>
 *   <li>{@code aud} nằm trong danh sách client ID CỦA MÌNH -- đây là điểm dễ bỏ sót nhất và cũng
 *       là điểm chết người nhất: một ID token hợp lệ hoàn toàn do Google cấp cho MỘT ỨNG DỤNG KHÁC
 *       vẫn qua được ba phép kiểm còn lại. Không có phép kiểm này thì bất kỳ ai có một app Google
 *       bất kỳ cũng đăng nhập được vào mọi tài khoản trong hệ thống;</li>
 *   <li>hạn dùng ({@code exp}/{@code iat}).</li>
 * </ul>
 *
 * <p>KHÔNG kiểm quyền và cũng không tra người dùng: cổng này chỉ trả lời "Google có thật sự nói
 * người cầm token này là địa chỉ email kia không". Việc email đó có phải người dùng đang hoạt động
 * của hệ thống hay không là câu hỏi của {@code OAuth2LoginUseCase}.
 */
public interface GoogleIdTokenVerifierPort {

    /**
     * @param idToken ID token thô (JWT) do client lấy được từ Google
     * @return danh tính đã kiểm chứng
     * @throws com.sep.vox.application.exception.UnauthorizedException khi token sai chữ ký, sai
     *         {@code iss}/{@code aud}, đã hết hạn, hoặc khi hệ thống chưa cấu hình danh sách
     *         {@code aud} được chấp nhận
     */
    VerifiedGoogleIdentity verify(String idToken);
}
