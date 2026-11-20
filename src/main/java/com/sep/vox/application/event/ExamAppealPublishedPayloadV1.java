package com.sep.vox.application.event;

import java.math.BigDecimal;
import java.util.UUID;

/** Kết quả phúc khảo đã công bố cho học sinh. */
public record ExamAppealPublishedPayloadV1(
    UUID appealId,
    UUID studentId,
    String examName,
    BigDecimal scoreBefore,
    BigDecimal scoreAfter
) {

}
