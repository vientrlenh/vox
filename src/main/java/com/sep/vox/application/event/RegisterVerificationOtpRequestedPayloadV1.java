package com.sep.vox.application.event;

/**
 * Yêu cầu gửi OTP xác thực đăng ký trường.
 *
 * <p>Payload KHÔNG mang mã OTP, cùng lý do với {@link ResetPasswordOtpRequestedPayloadV1}.
 * Consumer sinh mã, ghi bản hash vào dòng cache đăng ký (đã được use case tạo sẵn với
 * {@code otpHash} rỗng) rồi mới gửi mail.
 */
public record RegisterVerificationOtpRequestedPayloadV1(
    String to
) {
}
