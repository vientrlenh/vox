package com.sep.vox.application.port.input.query;

import java.util.UUID;

/** Báo cáo chất lượng AI; {@code examId} rỗng = toàn trường. */
public record ViewAiQualityReportQuery(
    UUID examId
) {
}
