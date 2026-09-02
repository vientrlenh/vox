package com.sep.vox.application.port.input.query;

import java.util.UUID;

/** Đơn phúc khảo của MỘT kỳ thi — màn của chủ tịch kỳ thi đó. */
public record SearchAppealsByExamQuery(
    UUID examId,
    String status,
    String keyword,
    int page,
    int size
) {
}
