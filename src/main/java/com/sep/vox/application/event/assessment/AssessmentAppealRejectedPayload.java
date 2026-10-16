package com.sep.vox.application.event.assessment;

import java.util.UUID;

/** Đơn phúc khảo bị từ chối ngay từ bước duyệt. */
public record AssessmentAppealRejectedPayload(
    UUID appealId,
    UUID studentId,
    String examName,
    String reason
) {

}
