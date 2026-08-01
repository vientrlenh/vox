package com.sep.vox.application.port.input.query;

import java.util.UUID;

/** Xuất bảng điểm; lọc theo kỳ thi và/hoặc ca thi, phạm vi luôn khoá trong trường. */
public record ExportExamScoresQuery(
    UUID examId,
    UUID scheduleId
) {
}
