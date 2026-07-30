package com.sep.vox.application.event;

import java.util.UUID;

/**
 * Payload của event UserCreated trên Kafka.
 *
 * KHÔNG chứa token đặt mật khẩu: token là credential, nếu nằm trong payload thì nó sẽ
 * tồn tại plaintext trong bảng outboxes và trong topic Kafka (retention thường dài hơn
 * tuổi thọ 48h của token). Consumer tự sinh token ngay trước khi gửi mail.
 */
public record UserCreatedPayloadV1(
    UUID userId,
    String to,
    String fullName,
    String schoolName,
    String userType
) {

}
