package com.sep.vox.application.query.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Một kỳ thi ĐÃ ĐÓNG mà vẫn còn bài chưa có điểm.
 *
 * <p>Lý do nó là một thẻ riêng chứ không phải một dòng trong danh sách kỳ thi: công bố điểm là cánh
 * cửa MỘT CHIỀU. {@code RetryGradingExamSessionUseCase} và {@code HandOffGradingToHumanUseCase} đều
 * từ chối khi kỳ thi ở {@code RESULTS_PUBLISHED}, nên bấm công bố lúc còn bài trống là chốt sổ vĩnh
 * viễn cho đúng những bài đó — không màn nào cảnh báo trước việc này.
 */
public record ExamAwaitingPublishDto(
    UUID examId,
    String code,
    String name,
    Instant closeAt,
    int unscoredCount,
    /** Trong số đó, còn cứu được bằng AI. */
    int aiFailedRetryLeft,
    /** Trong số đó, hết lượt AI — bắt buộc xếp giáo viên chấm. */
    int aiFailedNoRetryLeft,
    /** Trong số đó, đã nằm ở hàng đợi người chấm (chờ phân công hoặc đang chấm). */
    int awaitingHumanGrading
) {
}
