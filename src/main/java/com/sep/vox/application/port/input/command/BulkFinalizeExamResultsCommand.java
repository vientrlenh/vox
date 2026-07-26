package com.sep.vox.application.port.input.command;

import java.util.UUID;

/**
 * Chốt sổ toàn bộ kết quả của một kỳ thi.
 *
 * @param releasePendingWithAiScores xác nhận của admin rằng bài chưa ai chấm được
 *        công bố theo điểm AI đang có. Bắt buộc {@code true} khi preview còn bài chặn
 *        — mặc định {@code false} để không ai vô tình công bố điểm chưa được người xem.
 */
public record BulkFinalizeExamResultsCommand(
    UUID examId,
    boolean releasePendingWithAiScores
) {
}
