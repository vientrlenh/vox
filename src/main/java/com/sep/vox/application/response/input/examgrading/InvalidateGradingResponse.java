package com.sep.vox.application.response.input.examgrading;

import java.util.UUID;

public record InvalidateGradingResponse(
    UUID candidateResultId,
    String resultStatus
) {
}
