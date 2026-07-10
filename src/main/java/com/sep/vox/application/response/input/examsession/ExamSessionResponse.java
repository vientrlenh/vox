package com.sep.vox.application.response.input.examsession;

import java.util.UUID;

public record ExamSessionResponse(
    UUID id,
    UUID examId,
    UUID candidateId,
    UUID paperId,
    String startedAt,
    String submittedAt,
    String status
) {
}
