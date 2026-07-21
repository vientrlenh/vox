package com.sep.vox.application.event;

import java.math.BigDecimal;
import java.util.UUID;

public record ExamAppealPublishedEvent(
    UUID appealId,
    UUID studentId,
    String examName,
    BigDecimal scoreBefore,
    BigDecimal scoreAfter
) {
}
