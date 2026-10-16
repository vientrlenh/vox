package com.sep.vox.application.event.assessment;

import java.math.BigDecimal;
import java.util.UUID;

public record AssessmentAppealPublishedPayload(
    UUID appealId,
    UUID studentId,
    String examName,
    BigDecimal scoreBefore,
    BigDecimal scoreAfter
) {

}
