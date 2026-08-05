package com.sep.vox.application.port.input.query;

import com.sep.vox.domain.model.exam.ExamKind;

/**
 * Bộ lọc cho danh sách bài thi của học sinh. {@code status} là trạng thái suy ra để hiển thị
 * ({@code upcoming}/{@code in_progress}/{@code completed}) chứ không phải {@code ExamStatus} --
 * đúng với chuỗi mà {@code StudentExamViewSupport.statusOf} trả về cho FE.
 *
 * <p>{@code page} đánh số từ 0, khớp với {@code ViewExamsQuery} và {@code PageRequest.of}.
 */
public record ViewMyExamsQuery(
    ExamKind kind,
    String status,
    int page,
    int size,
    boolean sortDescending
) {
}
