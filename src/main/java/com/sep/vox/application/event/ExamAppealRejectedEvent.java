package com.sep.vox.application.event;

import java.util.UUID;

public record ExamAppealRejectedEvent(
    UUID appealId,
    UUID studentId,
    String examName,
    String reason
) {
}
