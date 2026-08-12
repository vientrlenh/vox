package com.sep.vox.application.event;

/**
 * Yêu cầu gửi OTP đặt lại mật khẩu.
 *
 * <p>Payload KHÔNG mang mã OTP -- xem {@code EventTypeConstant.RESET_PASSWORD_OTP_REQUESTED}.
 * Mã được sinh ở consumer ngay trước lúc gửi mail, nên nó không bao giờ tồn tại trong
 * {@code outboxes.payload} hay trong topic Kafka.
 */
public record ResetPasswordOtpRequestedPayloadV1(
    String to
) {
}
