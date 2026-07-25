package com.sep.vox.application.response.input.examitemresponse;

import java.util.UUID;

public record ExamSessionFollowupResponse(
    UUID examItemResponseId,
    long followupCount,
    long totalTurns
) {
}
