package com.sep.vox.interfaces.rest.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

/**
 * @param releasePendingWithAiScores xác nhận công bố bài chưa ai chấm theo điểm AI.
 *        Bỏ trống = {@code false}: mặc định an toàn, use case sẽ từ chối nếu còn bài dở.
 */
public record BulkFinalizeExamResultsRequest(
    @NotNull(message = "Thiếu kỳ thi cần chốt sổ")
    UUID examId,

    Boolean releasePendingWithAiScores
) {
}
