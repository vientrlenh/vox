package com.sep.vox.application.event;

import java.util.UUID;

/** Đơn phúc khảo bị từ chối ngay từ bước duyệt. */
public record AssessmentAppealRejectedPayloadV1(
    UUID appealId,
    UUID studentId,
    String assessmentName,
    String reason
) {

}
