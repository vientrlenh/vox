package com.sep.vox.application.event;

import java.math.BigDecimal;
import java.util.UUID;

/** The appeal result has been published to the student. */
public record AssessmentAppealPublishedPayloadV1(
    UUID appealId,
    UUID studentId,
    String assessmentName,
    BigDecimal scoreBefore,
    BigDecimal scoreAfter
) {

}
