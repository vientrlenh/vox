package com.sep.vox.application.event;

import java.util.UUID;

/** Đơn phúc khảo bị từ chối ngay từ bước duyệt. */
public record ExamAppealRejectedPayloadV1(
    UUID appealId,
    UUID studentId,
    String examName,
    String reason
) {

}
