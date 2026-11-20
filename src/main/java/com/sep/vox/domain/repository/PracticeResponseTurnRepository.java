package com.sep.vox.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface PracticeResponseTurnRepository {

    record TurnRecord(
        UUID id,
        int turnOrder,
        String turnType,
        String promptText,
        String audioUrl,
        String transcript,
        int durationSeconds,
        String wordFeedbackJson,
        Double turnScore
    ) {
    }

    /**
     * Kết quả ghi một lượt nói.
     *
     * @param turnId  id của dòng lượt -- dù mới ghi hay đã có sẵn
     * @param created false = Python gửi LẠI đúng lượt cũ (mất response HTTP rồi retry), không phải
     *                học sinh nói thêm một lượt
     */
    record TurnWrite(UUID turnId, boolean created) {
    }

    /**
     * Ghi một lượt nói, hoặc trả về lượt đã có nếu {@code (practiceResponseId, turnOrder)} đã được ghi
     * -- xem uq_practice_response_turn_order.
     *
     * <p>Trả về {@link TurnWrite} chứ KHÔNG chỉ trả id: bản thân bảng này idempotent, nhưng mọi việc
     * chạy sau nó trong SubmitPracticeTurnUseCase thì không -- trừ tiền vào ví, ghi dòng sửa lỗi, cộng
     * giây đã nói. Nuốt lặng thông tin "lượt này là bản gửi lại" nghĩa là ba thứ đó chạy thêm một lần
     * nữa mà không có dấu hiệu nào, vì thứ DUY NHẤT được bảo vệ lại đúng là thứ trông có vẻ đã chứng
     * minh rằng retry vô hại.
     */
    TurnWrite save(
        UUID practiceResponseId,
        int turnOrder,
        String turnType,
        String promptText,
        String audioUrl,
        String transcript,
        int durationSeconds,
        String wordFeedbackJson,
        Double turnScore
    );

    int findRemainingQuestionSeconds(UUID sessionId, UUID questionId);

    List<TurnRecord> findByPracticeResponseIdOrderByTurnOrder(UUID practiceResponseId);

    List<TurnRecord> findBySessionIdOrderByTurnOrder(UUID sessionId);

    /**
     * Tổng duration_seconds (giây trả lời thật) theo TỪNG practice_session_id trong sessionIds --
     * dùng cho QuotaPricingCalibrationService (nguồn PRACTICE), mirror
     * ExamItemResponseRepository.sumDurationSecondsGroupedBySessionIds.
     */
    List<SessionDurationAggregate> sumDurationSecondsGroupedBySessionIds(Collection<UUID> sessionIds);
}
