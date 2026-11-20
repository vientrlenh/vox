package com.sep.vox.application.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Đơn phúc khảo đã được duyệt và đã có người chấm.
 *
 * <p>Phát ở bước PHÂN CÔNG chứ không ở bước duyệt: giữa "đã duyệt" và "đã có người
 * chấm" có thể cách nhau nhiều ngày, và tin đáng gửi cho học sinh là tin thứ hai —
 * nó kèm được hạn xử lý cụ thể.
 */
public record ExamAppealApprovedPayloadV1(
    UUID appealId,
    UUID studentId,
    String examName,
    Instant deadline
) {

}
