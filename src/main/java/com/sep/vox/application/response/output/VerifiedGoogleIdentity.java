package com.sep.vox.application.response.output;

/**
 * Danh tính Google đã được xác minh -- chỉ những trường mà luồng đăng nhập thật sự dùng tới.
 *
 * <p>Đây là kết quả của một phép KIỂM CHỨNG mật mã, không phải một cục JSON do client gửi lên. Mọi
 * giá trị ở đây chỉ tồn tại sau khi chữ ký, {@code iss}, {@code aud} và hạn dùng của ID token đều đã
 * qua -- xem {@code GoogleIdTokenVerifierPort}. Nhờ vậy chỗ dùng không phải tự hỏi "tin được chưa",
 * và cũng không có đường nào để một trường chưa kiểm chứng lọt vào đây.
 *
 * @param subject       {@code sub} của Google -- định danh ổn định của tài khoản, không đổi kể cả
 *                      khi người dùng đổi email. Đi thẳng vào {@code OAuth2LoginCommand.providerUserId}.
 * @param email         email đã xác minh; đây là khoá tra cứu người dùng trong hệ thống
 * @param emailVerified Google KHÔNG bảo đảm mọi ID token đều có email đã xác minh (tài khoản
 *                      Workspace cấu hình lạ, hoặc token cấp cho scope không gồm email), nên trường
 *                      này được mang theo thay vì giả định -- chỗ gọi phải tự từ chối, y như
 *                      {@code CustomOidcUserService} làm cho đường web.
 * @param fullName      có thể null: {@code name} chỉ có khi token được cấp kèm scope {@code profile}
 * @param avatarUrl     có thể null, cùng lý do với {@code fullName}
 */
public record VerifiedGoogleIdentity(
    String subject,
    String email,
    boolean emailVerified,
    String fullName,
    String avatarUrl
) {
}
