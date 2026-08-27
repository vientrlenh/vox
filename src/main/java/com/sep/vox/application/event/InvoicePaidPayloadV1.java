package com.sep.vox.application.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.invoice.InvoiceSourceType;

/**
 * Hóa đơn đã thu tiền. Người nhận là mọi SCHOOL_ADMIN của trường.
 *
 * <p>{@code schoolAdminIds} được chốt ngay lúc phát sự kiện chứ không truy vấn lại ở
 * consumer: nhờ vậy mỗi lần chạy lại (retry, replay từ DLT) đều ra đúng tập người nhận cũ
 * và ràng buộc uk_notifications_user_event chặn được trùng. Nếu resolve tại consumer, một
 * admin mới thêm giữa hai lần retry sẽ nhận thông báo về hóa đơn đã cũ.
 *
 * <p>Phần chi tiết hạn mức đã mua KHÔNG nằm ở đây: chỉ mail cần tới nó, và nó cần vài lượt
 * truy vấn để dựng. Consumer mail tự lấy từ {@code subscriptionId}/{@code sourceId}.
 */
public record InvoicePaidPayloadV1(
    List<UUID> schoolAdminIds,
    UUID schoolId,
    UUID subscriptionId,
    UUID sourceId,
    String invoiceNumber,
    BigDecimal amount,
    Instant paidAt,
    InvoiceSourceType sourceType
) {
}
