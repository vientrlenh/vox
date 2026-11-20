package com.sep.vox.application.event;

import java.math.BigDecimal;
import java.util.UUID;

/** Kết quả phúc khảo đã công bố cho học sinh. */
public record AssessmentAppealPublishedPayloadV1(
    UUID appealId,
    UUID studentId,
    String assessmentName,
    BigDecimal scoreBefore,
    BigDecimal scoreAfter
) {

}
