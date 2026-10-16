package com.sep.vox.application.port.input.query;

import java.util.UUID;

/** Đơn phúc khảo của MỘT kỳ thi — màn của chủ tịch kỳ thi đó. */
public record SearchAppealsByAssessmentQuery(
    UUID assessmentId,
    String status,
    String keyword,
    int page,
    int size
) {
}
