package com.sep.vox.application.event;

import java.util.UUID;

/** An appeal was rejected at the approval step. */
public record AssessmentAppealRejectedPayloadV1(
    UUID appealId,
    UUID studentId,
    String assessmentName,
    String reason
) {

}
