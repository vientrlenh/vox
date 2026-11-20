package com.sep.vox.application.response.input.examitemresponse;

import java.util.List;
import java.util.UUID;

public record ExamItemResponseDetailsResponse(
    UUID id,
    UUID sessionId,
    UUID paperItemId,
    String audioUrl,
    Integer durationSeconds,
    String transcript,
    String submittedAt,
    List<ExamItemResponseTurnResponse> turns
) {
}
