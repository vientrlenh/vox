package com.sep.vox.application.response.input.assessmentitemresponse;

import java.util.UUID;

public record ExamSessionFollowupResponse(
    UUID examItemResponseId,
    long followupCount,
    long totalTurns
) {
}
