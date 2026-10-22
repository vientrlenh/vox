package com.sep.vox.application.port.input.query;

import com.sep.vox.application.shared.AppPageRequest;
import com.sep.vox.domain.model.assessment.AssessmentKind;

/**
 * Bộ lọc cho danh sách bài thi của học sinh. {@code status} là trạng thái suy ra để hiển thị
 * ({@code upcoming}/{@code in_progress}/{@code completed}) chứ không phải {@code AssessmentStatus} --
 * đúng với chuỗi mà {@code StudentAssessmentViewSupport.statusOf} trả về cho FE.
 *
 * <p>{@code pageRequest.page()} đếm từ 1; use case đổi sang 0-based bằng {@code repositoryPage()} khi gọi repository.
 */
public record ViewMyAssessmentsQuery(
    AssessmentKind kind,
    String status,
    AppPageRequest pageRequest,
    boolean sortDescending
) {
}
