package com.sep.vox.application.event;

import java.util.UUID;

public record ExamCandidateAddedEvent(
    UUID examId,
    UUID studentUserId
) {
}
