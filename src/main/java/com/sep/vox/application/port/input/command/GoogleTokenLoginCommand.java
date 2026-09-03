package com.sep.vox.application.port.input.command;

/**
 * Đăng nhập bằng ID token Google lấy được NATIVE trên máy người dùng.
 *
 * <p>Khác {@link OAuth2LoginCommand} ở chỗ mọi thông tin về người dùng còn nằm KHOÁ trong
 * {@code idToken} và chưa được kiểm chứng. {@code OAuth2LoginCommand} là thứ đi ra SAU khi kiểm --
 * ở đó email/sub đã là sự thật đã được xác minh, còn ở đây mới chỉ là một chuỗi do client gửi lên.
 * Hai record riêng chính là để không có đường nào nhầm cái này thành cái kia.
 */
public record GoogleTokenLoginCommand(
    String idToken,
    String ipAddress,
    String userAgent,
    ClientDeviceCommand device
) {
}
