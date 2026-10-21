package com.sep.vox.application.port.input.query;

import java.util.UUID;

import com.sep.vox.application.shared.AppPageRequest;

/** Đơn phúc khảo của MỘT kỳ thi — màn của chủ tịch kỳ thi đó. */
public record SearchAppealsByAssessmentQuery(
    UUID assessmentId,
    String status,
    String keyword,
    AppPageRequest pageRequest
) {
}
