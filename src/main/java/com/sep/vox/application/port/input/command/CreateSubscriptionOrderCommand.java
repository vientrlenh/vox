package com.sep.vox.application.port.input.command;

import java.util.UUID;

/**
 * Không mang schoolId: trường LẤY TỪ TOKEN của school admin đang đăng nhập. Nhận từ payload thì
 * @PreAuthorize("hasRole('SCHOOL_ADMIN')") không đủ để bảo vệ -- nó chỉ trả lời "có phải school admin
 * không", không trả lời "có phải school admin CỦA TRƯỜNG NÀY không", nên admin trường A vẫn đặt được
 * đơn cho trường B. Suy từ token thì không có gì để kiểm vì không có gì để khai sai.
 *
 * <p>Cũng không mang requestType/currentPlanId như SubmitRequestCommand cũ: cả hai đều suy ra được từ
 * gói đang ACTIVE của trường, mà suy ra thì không bao giờ lệch với sự thật.
 */
public record CreateSubscriptionOrderCommand(
    UUID subscriptionPlanId
) {
}
