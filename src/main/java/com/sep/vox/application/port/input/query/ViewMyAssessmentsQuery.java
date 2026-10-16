package com.sep.vox.application.port.input.query;

import com.sep.vox.domain.model.assessment.AssessmentKind;

/**
 * Bộ lọc cho danh sách bài thi của học sinh. {@code status} là trạng thái suy ra để hiển thị
 * ({@code upcoming}/{@code in_progress}/{@code completed}) chứ không phải {@code AssessmentStatus} --
 * đúng với chuỗi mà {@code StudentAssessmentViewSupport.statusOf} trả về cho FE.
 *
 * <p>{@code page} đánh số từ 0, khớp với {@code ViewAssessmentsQuery} và {@code PageRequest.of}.
 */
public record ViewMyAssessmentsQuery(
    AssessmentKind kind,
    String status,
    int page,
    int size,
    boolean sortDescending
) {
}
