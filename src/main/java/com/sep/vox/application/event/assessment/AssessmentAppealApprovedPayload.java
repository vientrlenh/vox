package com.sep.vox.application.event.assessment;

import java.time.Instant;
import java.util.UUID;

public record AssessmentAppealApprovedPayload(
    UUID appealId,
    UUID studentId,
    String examName,
    Instant deadline
) {

}
