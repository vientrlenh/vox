package com.sep.vox.application.port.input.command;

import java.util.UUID;

/**
 * KHÔNG có schoolId: gói đã biết nó thuộc trường nào, nên nhận thêm từ ngoài chỉ tạo ra một tham số
 * phải đối chiếu -- và một chỗ để đối chiếu sai.
 */
public record ForceSuspendSubscriptionCommand(
    UUID subscriptionId,
    String reason
) {
}
